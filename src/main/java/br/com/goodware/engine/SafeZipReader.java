package br.com.goodware.engine;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;

/** ZIP restritivo: verifica diretório central, cabeçalhos locais e CRC; nunca extrai no disco. */
public final class SafeZipReader {
 public static final int MAX_UPLOAD = 100 * 1024 * 1024;
 public static final int MAX_FILE = 10 * 1024 * 1024;
 public static final int MAX_TOTAL = 200 * 1024 * 1024;
 public static final int MAX_ENTRIES = 2000;
 private record Entry(String name, long crc, long packed, long size, int method, int flags, int offset) {}
 @FunctionalInterface public interface Visitor { void accept(String path, byte[] bytes); }
 public void read(byte[] zip, Visitor visitor, long deadline) {
  try {
   List<Entry> entries = inspect(zip);
   long total = 0;
   try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
    for (Entry expected : entries) {
     checkTime(deadline);
     ZipEntry actual = in.getNextEntry();
     if (actual == null || !actual.getName().equals(expected.name())) fail("Cabeçalhos ZIP inconsistentes.");
     ByteArrayOutputStream out = new ByteArrayOutputStream();
     byte[] buffer = new byte[8192];
     int read;
     while ((read = in.read(buffer)) != -1) {
      checkTime(deadline);
      total += read;
      if (out.size() + (long)read > MAX_FILE || total > MAX_TOTAL) fail("Limite de descompactação excedido.");
      out.write(buffer,0,read);
     }
     byte[] bytes = out.toByteArray();
     CRC32 crc = new CRC32(); crc.update(bytes);
     if (bytes.length != expected.size() || crc.getValue() != expected.crc()) fail("Tamanho ou CRC inválido.");
     if (actual.getCompressedSize() != expected.packed() || actual.getMethod() != expected.method()) fail("Metadados ZIP inconsistentes.");
     if (expected.name().endsWith("/")) {
      if (bytes.length != 0) fail("Diretório ZIP contém dados.");
     } else visitor.accept(expected.name(),bytes);
     in.closeEntry();
    }
    if (in.getNextEntry() != null) fail("Entradas ZIP não catalogadas.");
   }
  } catch (IOException | IllegalArgumentException ex) {
   throw new ScanException("ZIP inválido, corrompido ou com codificação não suportada.");
  }
 }
 private List<Entry> inspect(byte[] z) throws CharacterCodingException {
  if (z.length < 22 || z.length > MAX_UPLOAD) fail("ZIP vazio, inválido ou acima de 100 MiB.");
  int end = -1;
  for (int i=z.length-22; i>=Math.max(0,z.length-65557); i--) {
   if (u32(z,i)==0x06054b50L && i+22+u16(z,i+20)==z.length) { end=i; break; }
  }
  if (end < 0) fail("ZIP truncado ou sem diretório central.");
  int count = u16(z,end+10);
  long length=u32(z,end+12), start=u32(z,end+16);
  if (u16(z,end+4)!=0 || u16(z,end+6)!=0 || u16(z,end+8)!=count || count==65535 || start==0xffffffffL || length==0xffffffffL)
   fail("ZIP dividido ou ZIP64 não é suportado.");
  if (count==0) fail("O ZIP não contém arquivos.");
  if (count>MAX_ENTRIES) fail("O ZIP excede 2000 entradas.");
  if (start+length!=end || start>end) fail("Diretório central ZIP inválido.");
  int p=(int)start;
  long declaredTotal=0;
  Set<String> names=new HashSet<>();
  List<Entry> result=new ArrayList<>();
  for (int i=0;i<count;i++) {
   if (p+46>end || u32(z,p)!=0x02014b50L) fail("Diretório ZIP inválido.");
   int flags=u16(z,p+8), method=u16(z,p+10), n=u16(z,p+28), extra=u16(z,p+30), comment=u16(z,p+32);
   long packed=u32(z,p+20), size=u32(z,p+24), offset=u32(z,p+42);
   if (p+46L+n+extra+comment>end || offset>=start) fail("Entrada ZIP inválida.");
   if ((flags & ~0x080e)!=0 || (flags & 1)!=0 || (method!=0 && method!=8) || u16(z,p+34)!=0)
    fail("ZIP criptografado ou método de compressão não suportado.");
   int unixMode=(int)(u32(z,p+38)>>>16)&0170000;
   if (unixMode!=0 && unixMode!=0100000 && unixMode!=0040000) fail("Links e arquivos especiais não são aceitos.");
   String name = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
     .decode(ByteBuffer.wrap(z,p+46,n)).toString();
   validateName(name);
   String key=name.endsWith("/")?name.substring(0,name.length()-1):name;
   if (!names.add(key.toLowerCase(Locale.ROOT))) fail("Nomes duplicados ou ambíguos no ZIP.");
   if (size>MAX_FILE || (declaredTotal+=size)>MAX_TOTAL) fail("Limite de descompactação excedido.");
   if (size>1024*1024 && size>Math.max(1,packed)*100) fail("Razão de compressão excessiva (possível ZIP bomb).");
   result.add(new Entry(name,u32(z,p+16),packed,size,method,flags,(int)offset));
   p+=46+n+extra+comment;
  }
  if(p!=end) fail("Tamanho do diretório ZIP inconsistente.");
  result.sort(Comparator.comparingInt(Entry::offset));
  int expectedOffset=0;
  for (Entry e:result) {
   int o=e.offset();
   if(o!=expectedOffset || o+30L>start || u32(z,o)!=0x04034b50L) fail("Cabeçalho local ZIP inválido.");
   int n=u16(z,o+26), extra=u16(z,o+28);
   long data=o+30L+n+extra;
   if(data+e.packed()>start || u16(z,o+6)!=e.flags() || u16(z,o+8)!=e.method()) fail("Entrada ZIP sobreposta ou inconsistente.");
   String name=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(z,o+30,n)).toString();
   if(!name.equals(e.name())) fail("Nome local difere do diretório ZIP.");
   long next=data+e.packed();
   if((e.flags() & 8)!=0) {
    if(next+12>start) fail("Descritor ZIP truncado.");
    int d=(int)next;
    if(u32(z,d)==0x08074b50L) {d+=4;next+=4;}
    if(next+12>start || u32(z,d)!=e.crc() || u32(z,d+4)!=e.packed() || u32(z,d+8)!=e.size()) fail("Descritor ZIP inconsistente.");
    next+=12;
   } else if(u32(z,o+14)!=e.crc() || u32(z,o+18)!=e.packed() || u32(z,o+22)!=e.size()) fail("Tamanho local ZIP inconsistente.");
   expectedOffset=(int)next;
  }
  if(expectedOffset!=start) fail("Dados extras entre entradas ZIP.");
  return result;
 }
 private static void validateName(String name) {
  if(name.isBlank() || name.length()>240 || name.startsWith("/") || name.contains("\\") || name.contains(":")) fail("Caminho ZIP inválido.");
  if(name.codePoints().anyMatch(c -> Character.isISOControl(c) || Character.getType(c)==Character.FORMAT)) fail("Nome ZIP contém caracteres de controle.");
  String[] segments=name.split("/",-1);
  if(segments.length>21) fail("Profundidade de diretórios excede 20.");
  for(int i=0;i<segments.length;i++) {
   String s=segments[i];
   if(s.equals(".") || s.equals("..") || (s.isEmpty() && i!=segments.length-1) || s.endsWith(" ") || s.endsWith(".")) fail("Caminho ZIP ambíguo ou com path traversal.");
  }
 }
 public static void checkTime(long deadline) { if(System.nanoTime()>deadline) fail("Tempo de análise excedido. Divida o projeto em pacotes menores."); }
 private static int u16(byte[] z,int p) { return (z[p]&255)|((z[p+1]&255)<<8); }
 private static long u32(byte[] z,int p) { return Integer.toUnsignedLong((z[p]&255)|((z[p+1]&255)<<8)|((z[p+2]&255)<<16)|((z[p+3]&255)<<24)); }
 private static void fail(String message) { throw new ScanException(message); }
}

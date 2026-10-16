package br.com.goodware.engine;
import java.io.*;
import java.util.*;
import java.util.zip.*;
public final class ZipFixtures {
 public static byte[] zip(Map<String,byte[]> files) throws IOException {
  return zip(files,false);
 }
 public static byte[] zip(Map<String,byte[]> files,boolean stored) throws IOException {
  ByteArrayOutputStream out=new ByteArrayOutputStream();
  try(ZipOutputStream z=new ZipOutputStream(out)) {
   for(var f:files.entrySet()) {ZipEntry e=new ZipEntry(f.getKey());
    if(stored) {CRC32 crc=new CRC32();crc.update(f.getValue());e.setMethod(ZipEntry.STORED);e.setSize(f.getValue().length);e.setCompressedSize(f.getValue().length);e.setCrc(crc.getValue());}
    z.putNextEntry(e);z.write(f.getValue());z.closeEntry();
   }
  }
  return out.toByteArray();
 }
 public static int central(byte[] b) {for(int i=0;i<b.length-4;i++)if(b[i]==80 && b[i+1]==75 && b[i+2]==1 && b[i+3]==2)return i;throw new IllegalArgumentException();}
}

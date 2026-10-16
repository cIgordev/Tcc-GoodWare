package br.com.goodware.engine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SafeZipReaderTest {
 private void scan(byte[] z) {new SafeZipReader().read(z,(p,b)->{},System.nanoTime()+30_000_000_000L);}
 @Test void acceptsStoredAndDeflatedAndCountsOnlyVisitorFiles() throws Exception {
  for(boolean stored:List.of(false,true)) {
   List<String> seen=new ArrayList<>();byte[] zip=ZipFixtures.zip(Map.of("src/",new byte[0],"src/Olá.java","class Olá {}".getBytes(java.nio.charset.StandardCharsets.UTF_8)),stored);
   new SafeZipReader().read(zip,(p,b)->seen.add(p),System.nanoTime()+30_000_000_000L);assertEquals(List.of("src/Olá.java"),seen);
  }
 }
 @ParameterizedTest @ValueSource(strings={"../escape.java","/absolute.java","C:/windows.java","a/../b.java","a\\b.java","a/./b.java","a//b.java","a. /b.java","a\u0001.txt"})
 void rejectsUnsafeNames(String name)throws Exception {assertThrows(ScanException.class,()->scan(ZipFixtures.zip(Map.of(name,new byte[0]))));}
 @Test void rejectsNonZipAndEmpty()throws Exception {assertThrows(ScanException.class,()->scan("Not a ZIP archive pretending to be valid".getBytes()));assertThrows(ScanException.class,()->scan(ZipFixtures.zip(Map.of())));}
 @Test void rejectsTruncationAndTrailingData()throws Exception {
  byte[] z=ZipFixtures.zip(Map.of("a.txt","hello".getBytes()));
  assertThrows(ScanException.class,()->scan(Arrays.copyOf(z,z.length-5)));
  assertThrows(ScanException.class,()->scan(Arrays.copyOf(z,z.length+4)));
 }
 @Test void rejectsCrcDamage()throws Exception {
  byte[] z=ZipFixtures.zip(Map.of("a.txt","hello".getBytes()),true);z[35]^=1;
  assertThrows(ScanException.class,()->scan(z));
 }
 @Test void rejectsLocalCentralNameMismatch()throws Exception {byte[] z=ZipFixtures.zip(Map.of("a.txt",new byte[0]));z[30]='b';assertThrows(ScanException.class,()->scan(z));}
 @Test void rejectsSymlink()throws Exception {byte[] z=ZipFixtures.zip(Map.of("link","target".getBytes()));int c=ZipFixtures.central(z);z[c+41]=(byte)0xa0;assertThrows(ScanException.class,()->scan(z));}
 @Test void rejectsEncryption()throws Exception {byte[] z=ZipFixtures.zip(Map.of("a",new byte[0]));z[ZipFixtures.central(z)+8]|=1;assertThrows(ScanException.class,()->scan(z));}
 @Test void rejectsCaseAliases()throws Exception {assertThrows(ScanException.class,()->scan(ZipFixtures.zip(Map.of("a.txt",new byte[0],"A.txt",new byte[0]))));}
 @Test void rejectsDepth()throws Exception {assertThrows(ScanException.class,()->scan(ZipFixtures.zip(Map.of("a/".repeat(21)+"b",new byte[0]))));}
 @Test void rejectsEntryCount()throws Exception {Map<String,byte[]> f=new HashMap<>();for(int i=0;i<2001;i++)f.put(i+".txt",new byte[0]);assertThrows(ScanException.class,()->scan(ZipFixtures.zip(f)));}
 @Test void rejectsCompressionBomb()throws Exception {byte[] zeros=new byte[2*1024*1024];assertThrows(ScanException.class,()->scan(ZipFixtures.zip(Map.of("bomb.txt",zeros))));}
 @Test void rejectsEntrySize()throws Exception {byte[] z=ZipFixtures.zip(Map.of("a",new byte[0]));int c=ZipFixtures.central(z);z[c+27]=1;assertThrows(ScanException.class,()->scan(z));}
 @Test void enforcesDeadline()throws Exception {byte[] z=ZipFixtures.zip(Map.of("a",new byte[0]));assertThrows(ScanException.class,()->new SafeZipReader().read(z,(p,b)->{},0));}
 @Test void rejectsSplitZip()throws Exception {byte[] z=ZipFixtures.zip(Map.of("a",new byte[0]));z[z.length-18]=1;assertThrows(ScanException.class,()->scan(z));}
 @Test void rejectsForgedOffsets()throws Exception {byte[] z=ZipFixtures.zip(Map.of("a",new byte[0]));z[ZipFixtures.central(z)+42]=10;assertThrows(ScanException.class,()->scan(z));}
}

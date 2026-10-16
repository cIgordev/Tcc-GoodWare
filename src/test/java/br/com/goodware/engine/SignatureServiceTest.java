package br.com.goodware.engine;
import br.com.goodware.service.SignatureService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
class SignatureServiceTest {
 private SignatureService service(String type,String value,String classification)throws Exception {
  var mapper=new ObjectMapper();var sig=new SignatureService.Signature("FIXTURE",type,value,"Fixture sintética, não é ameaça real","https://example.org/test-fixtures","2026-10-14","HIGH",classification,"CC0 - teste interno");
  byte[] json=mapper.writeValueAsBytes(new SignatureService.Catalog("test",java.util.List.of(sig)));
  return new SignatureService(mapper,new ByteArrayInputStream(json));
 }
 @Test void emptyProductionCatalog(){assertEquals(0,new SignatureService(new ObjectMapper()).size());}
 @Test void exactHashAndExplicitTestClassification()throws Exception {String hash=Hashes.sha256("harmless fixture".getBytes(StandardCharsets.UTF_8));var s=service("SHA256",hash,"TEST");var f=s.scanHash("a",hash);assertEquals("TEST_SIGNATURE",f.getFirst().category());assertTrue(s.scanHash("b",Hashes.sha256("different".getBytes())).isEmpty());}
 @Test void urlHostBoundaries()throws Exception {var s=service("DOMAIN","example.org","TEST");assertEquals(1,s.scanText("a.txt","https://example.org/path").size());assertTrue(s.scanText("a.txt","https://example.org.attacker.test/path").isEmpty());assertTrue(s.scanText("a.txt","https://notexample.org/path").isEmpty());}
 @Test void rejectsInventedMalformedHash(){assertThrows(IllegalStateException.class,()->service("SHA256","bad","TEST"));}
 @Test void rejectsInvalidClassification(){assertThrows(IllegalStateException.class,()->service("DOMAIN","example.org","unknown"));}
 @Test void urlsAreComparedWithoutFetching()throws Exception {var s=service("URL","https://example.org/a","TEST");assertEquals(1,s.scanText("a","https://example.org/a").size());assertTrue(s.scanText("a","https://example.org/b").isEmpty());}
}

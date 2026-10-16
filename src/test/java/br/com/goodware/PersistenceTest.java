package br.com.goodware;
import br.com.goodware.engine.*;
import br.com.goodware.model.*;
import br.com.goodware.persistence.AnalysisRepository;
import br.com.goodware.service.AnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class PersistenceTest {
 @org.junit.jupiter.api.BeforeEach void owner(){br.com.goodware.auth.OwnerContext.set(UUID.fromString("10000000-0000-4000-8000-000000000001"));}
 @org.junit.jupiter.api.AfterEach void clear(){br.com.goodware.auth.OwnerContext.clear();}

 @Autowired AnalysisRepository repository;
 @Autowired AnalysisService service;
 @Autowired JdbcTemplate jdbc;
 @Autowired ObjectMapper mapper;
 private MockMultipartFile zip(String value)throws Exception{return new MockMultipartFile("file","synthetic.zip","application/zip",ZipFixtures.zip(Map.of("a.java",value.getBytes(java.nio.charset.StandardCharsets.UTF_8))));}
 @Test void freshRepositoryRecoversResultAndRelations()throws Exception {
  var result=service.analyze(zip("Runtime.getRuntime().exec(input);"));
  var fresh=new AnalysisRepository(jdbc,mapper);
  assertEquals(result.id(),fresh.find(result.id()).orElseThrow().id());
  assertEquals(1,fresh.find(result.id()).orElseThrow().files().size());
  assertEquals(1,fresh.find(result.id()).orElseThrow().findings().size());
  assertEquals("COMPLETED",fresh.state(result.id()).get("status"));
  assertNotNull(fresh.state(result.id()).get("completedAt"));
  assertEquals(1,fresh.summary(result.id()).size());
 }
 @Test void deleteCascades()throws Exception {
  var r=service.analyze(zip("Runtime.getRuntime().exec(input);"));assertTrue(repository.delete(r.id()));
  for(String table:List.of("analyzed_files","findings"))assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM goodware."+table+" WHERE analysis_id=?",Integer.class,UUID.fromString(r.id())));
 }
 @Test void failedCompletionRollsBackAllChildren(){
  String id=UUID.randomUUID().toString();repository.start(id,"SYNTHETIC","a".repeat(64),100,Instant.now());
  var file=new FileReport("a.java",10,"b".repeat(64),"java","TEXT_SCANNED","fixture",0);
  var bad=new Finding("test","POTENTIAL_VULNERABILITY","HIGH","MEDIUM","fixture","missing.java",1,"TEST","fixture","fixture","fixture");
  var result=new AnalysisResult(id,"SYNTHETIC","COMPLETED","a".repeat(64),Instant.now(),1,0,1,0,0,List.of(bad),List.of(file),List.of(),"test","test",0,1);
  assertThrows(org.springframework.dao.DataAccessException.class,()->repository.complete(result));
  assertTrue(repository.find(id).orElseThrow().files().isEmpty());
  assertEquals("RUNNING",repository.state(id).get("status"));repository.fail(id);
  assertEquals("FAILED",repository.state(id).get("status"));
 }
 @Test void malformedZipRecordsFailureWithoutChildren()throws Exception {
  long before=repository.count();
  assertThrows(ScanException.class,()->service.analyze(new MockMultipartFile("file","bad.zip","application/zip",new byte[]{1,2,3})));
  assertEquals(before+1,repository.count());
  var r=repository.list(0,1).getFirst();assertEquals("FAILED",r.status());assertTrue(r.files().isEmpty());
 }
 @Test void redactedEvidenceIsPersisted()throws Exception {
  var r=service.analyze(zip("String password = \"synthetic-sensitive-value\";"));
  assertFalse(r.findings().isEmpty());
  assertFalse(repository.find(r.id()).orElseThrow().toString().contains("synthetic-sensitive-value"));
 }
 @Test void historyValidationAndStablePages()throws Exception {
  for(int i=0;i<3;i++)service.analyze(zip("class Synthetic"+i+" {}"));
  var a=repository.list(0,2);var b=repository.list(1,2);
  assertEquals(2,a.size());assertTrue(a.stream().noneMatch(x->b.stream().anyMatch(y->x.id().equals(y.id()))));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.history(-1,20));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.history(0,101));
 }
 @Test void recoversInterruptedButPreservesRecent(){
  String old=UUID.randomUUID().toString(),recent=UUID.randomUUID().toString();
  repository.start(old,"SYNTHETIC","a".repeat(64),1,Instant.now().minusSeconds(700));
  repository.start(recent,"SYNTHETIC","a".repeat(64),1,Instant.now());
  repository.recover();
  assertEquals("FAILED",repository.state(old).get("status"));assertEquals("RUNNING",repository.state(recent).get("status"));
  repository.fail(recent);
 }
 @Test void catalogSnapshotHasProvenanceAndIsImmutable(){
  var entry=new br.com.goodware.service.SignatureService.Signature("SYNTHETIC","SHA256","a".repeat(64),"Synthetic fixture only","https://example.invalid/synthetic","2026-10-14","LOW","TEST","Synthetic test");
  var catalog=new br.com.goodware.service.SignatureService.Catalog("synthetic-test-v1",List.of(entry));
  repository.snapshot(catalog);repository.snapshot(catalog);
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM goodware.signature_catalog WHERE catalog_version='synthetic-test-v1'",Integer.class));
  var changed=new br.com.goodware.service.SignatureService.Signature("SYNTHETIC","SHA256","b".repeat(64),"Synthetic fixture only","https://example.invalid/synthetic","2026-10-14","LOW","TEST","Synthetic test");
  assertThrows(IllegalStateException.class,()->repository.snapshot(new br.com.goodware.service.SignatureService.Catalog("synthetic-test-v1",List.of(changed))));
 }
 @Test void runningCannotBeDeleted(){String id=UUID.randomUUID().toString();repository.start(id,"SYNTHETIC","a".repeat(64),1,Instant.now());assertFalse(repository.delete(id));repository.fail(id);assertTrue(repository.delete(id));}
}

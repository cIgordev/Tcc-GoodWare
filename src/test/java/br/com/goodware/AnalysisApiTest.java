package br.com.goodware;
import br.com.goodware.engine.*;
import br.com.goodware.service.AnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @AutoConfigureMockMvc
class AnalysisApiTest {
 @org.springframework.test.context.bean.override.mockito.MockitoBean br.com.goodware.auth.AuthGateway auth;
 @Autowired org.springframework.web.context.WebApplicationContext context;
 @org.junit.jupiter.api.BeforeEach void authenticate(){
  var owner=java.util.UUID.fromString("10000000-0000-4000-8000-000000000001");
  org.mockito.Mockito.when(auth.verify("synthetic-token")).thenReturn(owner);
  var session=new org.springframework.mock.web.MockHttpSession();session.setAttribute("token","synthetic-token");session.setAttribute("expires",java.time.Instant.now().getEpochSecond()+1800);
  mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
   .addFilters(context.getBean(br.com.goodware.controller.LocalSecurityFilter.class),context.getBean(br.com.goodware.auth.AuthenticationFilter.class))
   .defaultRequest(get("/").session(session).header("X-GoodWare-Request","1")).build();
  br.com.goodware.auth.OwnerContext.set(owner);
 }
 @org.junit.jupiter.api.AfterEach void clear(){br.com.goodware.auth.OwnerContext.clear();}

 @Autowired MockMvc mvc;
 @Autowired ObjectMapper mapper;
 @Autowired AnalysisService service;
 private MockMultipartFile fixture(String text)throws Exception {return new MockMultipartFile("file","project.zip","application/zip",ZipFixtures.zip(Map.of("src/A.java",text.getBytes(java.nio.charset.StandardCharsets.UTF_8))));}
 @Test void realUploadReportInventoryHashAndDelete()throws Exception {
  var upload=fixture("Runtime.getRuntime().exec(input);");
  String json=mvc.perform(multipart("/api/analyses").file(upload)).andExpect(status().isCreated()).andExpect(jsonPath("$.filesAnalyzed").value(1)).andExpect(jsonPath("$.findings[0].ruleId").value("GW-JAVA-EXEC-001")).andReturn().getResponse().getContentAsString();
  var r=mapper.readTree(json);String id=r.get("id").asText(),hash=r.get("sha256").asText();
  assertEquals(Hashes.sha256(upload.getBytes()),hash);
  mvc.perform(get("/api/analyses/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.files[0].status").value("TEXT_SCANNED"));
  mvc.perform(get("/api/analyses/"+id+"/files")).andExpect(status().isOk()).andExpect(jsonPath("$[0].path").value("src/A.java"));
  mvc.perform(get("/api/analyses/"+id+"/findings")).andExpect(status().isOk()).andExpect(jsonPath("$[0].line").value(1));
  mvc.perform(get("/api/analyses/"+id+"/report")).andExpect(status().isOk()).andExpect(header().string("Content-Disposition","attachment; filename=goodware-report.json"));
  mvc.perform(get("/api/analyses").param("sha256",hash)).andExpect(status().isOk());
  mvc.perform(delete("/api/analyses/"+id)).andExpect(status().isNoContent());
  mvc.perform(get("/api/analyses/"+id)).andExpect(status().isNotFound());
 }
 @Test void safeFileAndDirectoryCount()throws Exception {
  var file=new MockMultipartFile("file","safe.zip","application/zip",ZipFixtures.zip(Map.of("src/",new byte[0],"src/A.java","class A {}".getBytes())));
  mvc.perform(multipart("/api/analyses").file(file)).andExpect(status().isCreated()).andExpect(jsonPath("$.findings").isEmpty()).andExpect(jsonPath("$.filesAnalyzed").value(1));
 }
 @Test void invalidUploadHasSanitizedError()throws Exception {
  mvc.perform(multipart("/api/analyses").file(new MockMultipartFile("file","x.zip","application/zip","definitely not a zip".getBytes())))
   .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").exists()).andExpect(jsonPath("$.trace").doesNotExist());
 }
 @Test void missingFileAndBadHash()throws Exception {mvc.perform(multipart("/api/analyses")).andExpect(status().isBadRequest());mvc.perform(get("/api/analyses").param("sha256","bad")).andExpect(status().isBadRequest());}
 @Test void wrongExtensionAndEmpty()throws Exception {
  mvc.perform(multipart("/api/analyses").file(new MockMultipartFile("file","x.txt","text/plain","hello".getBytes()))).andExpect(status().isBadRequest());
  mvc.perform(multipart("/api/analyses").file(new MockMultipartFile("file","x.zip","application/zip",new byte[0]))).andExpect(status().isBadRequest());
 }
 @Test void refusesCrossOriginAndRebinding()throws Exception {
  mvc.perform(multipart("/api/analyses").file(fixture("class A {}" )).header("Origin","https://evil.example")).andExpect(status().isForbidden());
  mvc.perform(get("/api/health").header("Sec-Fetch-Site","cross-site")).andExpect(status().isForbidden());
  mvc.perform(get("/api/health").with(r->{r.setServerName("evil.example");return r;})).andExpect(status().isForbidden());
 }
 @Test void emitsSecurityHeadersAndServesFrontend()throws Exception {mvc.perform(get("/analisar.html")).andExpect(status().isOk()).andExpect(header().string("X-Content-Type-Options","nosniff")).andExpect(header().exists("Content-Security-Policy"));}
 @Test void binaryAndNonUtf8AreExplicitlySkipped()throws Exception {
  var file=new MockMultipartFile("file","binary.zip","application/zip",ZipFixtures.zip(Map.of("a.exe",new byte[]{0,1,2},"b.java",new byte[]{(byte)0xff})));
  mvc.perform(multipart("/api/analyses").file(file)).andExpect(status().isCreated()).andExpect(jsonPath("$.files[0].status").value("HASH_ONLY")).andExpect(jsonPath("$.files[1].status").value("HASH_ONLY"));
 }
 @Test void historyRetainsMoreThanThirtyAndPaginates()throws Exception {for(int i=0;i<32;i++)service.analyze(fixture("class A {} // "+i));assertEquals(30,service.list().size());assertTrue((long)service.history(0,100).get("total")>=32);assertFalse(((java.util.List<?>)service.history(1,30).get("items")).isEmpty());}
 @Test void skippedLongLineIsReported()throws Exception {var r=service.analyze(fixture("a".repeat(5000)));assertEquals("PARTIAL",r.files().getFirst().status());}
 @Test void directoriesOnlyIsNotASuccess()throws Exception {
  mvc.perform(multipart("/api/analyses").file(new MockMultipartFile("file","dirs.zip","application/zip",ZipFixtures.zip(Map.of("src/",new byte[0]))))).andExpect(status().isBadRequest());
 }
}

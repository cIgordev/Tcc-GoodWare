package br.com.goodware;
import br.com.goodware.auth.*;
import br.com.goodware.engine.ZipFixtures;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @AutoConfigureMockMvc
class AuthIsolationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper mapper;
 @MockitoBean AuthGateway auth;
 MockHttpSession a,b;
 MockHttpSession session(String token){var s=new MockHttpSession();s.setAttribute("token",token);s.setAttribute("expires",java.time.Instant.now().getEpochSecond()+1800);return s;}
 @BeforeEach void setup(){
  when(auth.verify("A")).thenReturn(UUID.fromString("20000000-0000-4000-8000-000000000001"));
  when(auth.verify("B")).thenReturn(UUID.fromString("20000000-0000-4000-8000-000000000002"));a=session("A");b=session("B");
 }
 @Test void anonymousAndForgedOwnerDenied()throws Exception{
  mvc.perform(get("/api/analyses").header("X-User-Id","20000000-0000-4000-8000-000000000001")).andExpect(status().isUnauthorized());
  mvc.perform(multipart("/api/analyses").header("X-GoodWare-Request","1")).andExpect(status().isUnauthorized());
 }
 @Test void everyReportRouteAndHashEnforceOwner()throws Exception{
  var file=new MockMultipartFile("file","synthetic.zip","application/zip",ZipFixtures.zip(Map.of("a.java","Runtime.getRuntime().exec(input);".getBytes())));
  var r=mapper.readTree(mvc.perform(multipart("/api/analyses").file(file).session(a).header("X-GoodWare-Request","1"))
   .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
  String id=r.path("id").asText();
  for(String suffix:List.of("","/status","/summary","/findings","/files","/report")){
   mvc.perform(get("/api/analyses/"+id+suffix).session(b)).andExpect(status().isNotFound());
   mvc.perform(get("/api/analyses/"+id+suffix).session(a)).andExpect(status().isOk());
  }
  mvc.perform(get("/api/analyses").param("sha256",r.path("sha256").asText()).session(b)).andExpect(status().isNotFound());
  mvc.perform(get("/api/analyses/history").session(b)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
  mvc.perform(delete("/api/analyses/"+id).session(b).header("X-GoodWare-Request","1")).andExpect(status().isNotFound());
  mvc.perform(delete("/api/analyses/"+id).session(a).header("X-GoodWare-Request","1")).andExpect(status().isNoContent());
  assertThrows(org.springframework.web.server.ResponseStatusException.class,OwnerContext::require);
 }
 @Test void expiredAndInvalidSessionsFailClosed()throws Exception{
  a.setAttribute("expires",0L);mvc.perform(get("/api/analyses").session(a)).andExpect(status().isUnauthorized());
  when(auth.verify("B")).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED,"Sessão inválida."));
  mvc.perform(get("/api/analyses").session(b)).andExpect(status().isUnauthorized());
 }
 @Test void csrfIsRequiredEvenOnLogin()throws Exception{
  mvc.perform(post("/api/auth/login").contentType("application/json").content("{}" )).andExpect(status().isForbidden());
  mvc.perform(post("/api/auth/logout").session(a).header("X-GoodWare-Request","1").header("Origin","https://evil.invalid")).andExpect(status().isForbidden());
 }
 @Test void loginReplacesSessionAndDoesNotReturnTokens()throws Exception{
  when(auth.login("synthetic@example.invalid","synthetic-password")).thenReturn(mapper.readTree("{\"access_token\":\"A\",\"expires_in\":3600}"));
  mvc.perform(post("/api/auth/login").session(b).header("X-GoodWare-Request","1").contentType("application/json").content("{\"email\":\"synthetic@example.invalid\",\"password\":\"synthetic-password\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.access_token").doesNotExist());
  assertTrue(b.isInvalid());
 }
 @Test void logoutInvalidatesLocalSession()throws Exception{
  mvc.perform(post("/api/auth/logout").session(a).header("X-GoodWare-Request","1")).andExpect(status().isOk());assertTrue(a.isInvalid());verify(auth).logout("A");
 }
}

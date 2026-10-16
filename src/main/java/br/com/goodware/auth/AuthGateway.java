package br.com.goodware.auth;
import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
@Service
public class AuthGateway {
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
 private final ObjectMapper mapper; private final String url,key;
 public AuthGateway(ObjectMapper mapper,@Value("${goodware.auth.url}") String url,@Value("${goodware.auth.publishable-key}") String key){
  this.mapper=mapper;this.url=url;this.key=key;
  if(!url.equals("https://tvnvjfaxuxwndlsnpiar.supabase.co"))throw new IllegalArgumentException("Projeto Auth não autorizado.");
 }
 public JsonNode login(String email,String password){return call("/token?grant_type=password","POST",Map.of("email",email,"password",password),null);}
 public void signup(String email,String password){call("/signup","POST",Map.of("email",email,"password",password),null);}
 public UUID verify(String token){
  var user=call("/user","GET",null,token);
  try{if(user.path("is_anonymous").asBoolean(false))throw new IllegalArgumentException();return UUID.fromString(user.path("id").asText());}
  catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sessão inválida.");}
 }
 public void logout(String token){call("/logout?scope=local","POST",Map.of(),token);}
 private JsonNode call(String path,String method,Object body,String token){
  try{
   var b=HttpRequest.newBuilder(URI.create(url+"/auth/v1"+path)).timeout(Duration.ofSeconds(10)).header("apikey",key).header("Content-Type","application/json");
   if(token!=null)b.header("Authorization","Bearer "+token);
   b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
   var r=client.send(b.build(),HttpResponse.BodyHandlers.ofString());
   if(r.statusCode()==429)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Muitas tentativas. Aguarde.");
   if(r.statusCode()>=500)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Autenticação indisponível.");
   if(r.statusCode()>=400)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Não foi possível autenticar. Confira os dados e a confirmação do email.");
   return r.body().isBlank()?mapper.createObjectNode():mapper.readTree(r.body());
  }catch(ResponseStatusException e){throw e;}catch(InterruptedException e){Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Autenticação indisponível.");}
   catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Autenticação indisponível.");}
 }
}

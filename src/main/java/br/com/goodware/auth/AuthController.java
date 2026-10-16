package br.com.goodware.auth;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.Map;
@RestController
@RequestMapping("/api/auth")
public class AuthController {
 private final AuthGateway auth;
 private long window;private int attempts;
 public AuthController(AuthGateway auth){this.auth=auth;}
 public record Credentials(@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=8,max=128) String password){}
 private synchronized void limit(){long now=System.currentTimeMillis()/60000;if(now!=window){window=now;attempts=0;}if(++attempts>30)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Muitas tentativas. Aguarde um minuto.");}
 @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody Credentials c,HttpServletRequest req){
  limit();var result=auth.login(c.email(),c.password());String token=result.path("access_token").asText();
  var owner=auth.verify(token);var old=req.getSession(false);if(old!=null)old.invalidate();
  var session=req.getSession(true);session.setMaxInactiveInterval(1800);session.setAttribute("token",token);
  session.setAttribute("expires",Instant.now().getEpochSecond()+Math.min(1800,result.path("expires_in").asLong(0)));
  return Map.of("id",owner.toString());
 }
 @PostMapping("/signup") public Map<String,String> signup(@Valid @RequestBody Credentials c){limit();auth.signup(c.email(),c.password());return Map.of("message","Se o cadastro estiver disponível, confira seu email para confirmar a conta. Depois entre.");}
 @GetMapping("/me") public Map<String,String> me(){return Map.of("id",OwnerContext.require().toString());}
 @PostMapping("/logout") public Map<String,String> logout(HttpServletRequest req){
  var s=req.getSession(false);if(s!=null){String token=(String)s.getAttribute("token");s.invalidate();if(token!=null)auth.logout(token);}return Map.of("message","Sessão encerrada.");
 }
}

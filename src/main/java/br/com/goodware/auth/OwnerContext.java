package br.com.goodware.auth;
import java.util.UUID;
/** Only the authentication filter populates this context; never accept ownership from input. */
public final class OwnerContext {
 private static final ThreadLocal<UUID> CURRENT=new ThreadLocal<>();
 private OwnerContext(){}
 public static UUID require(){var id=CURRENT.get();if(id==null)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED,"Entre na sua conta.");return id;}
 public static void set(UUID id){CURRENT.set(id);}
 public static void clear(){CURRENT.remove();}
}

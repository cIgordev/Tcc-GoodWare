package br.com.goodware.auth;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
/** Refuse production credentials which would bypass ownership policies. */
@Component
public class DatabaseRoleGuard implements ApplicationRunner {
 private final JdbcTemplate jdbc;
 public DatabaseRoleGuard(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @Override public void run(ApplicationArguments args){
  Boolean pg=jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>) c->c.getMetaData().getDatabaseProductName().equals("PostgreSQL"));
  if(!Boolean.TRUE.equals(pg))return;
  Boolean unsafe=jdbc.queryForObject("SELECT r.rolsuper OR r.rolbypassrls OR pg_has_role(current_user,c.relowner,'USAGE') FROM pg_roles r CROSS JOIN pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE r.rolname=current_user AND n.nspname='goodware' AND c.relname='analyses'",Boolean.class);
  if(!Boolean.FALSE.equals(unsafe))throw new IllegalStateException("Use um login restrito goodware_backend, sem propriedade de tabelas ou BYPASSRLS.");
 }
}

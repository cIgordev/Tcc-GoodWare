package br.com.goodware.persistence;

import br.com.goodware.engine.Evidence;
import br.com.goodware.model.*;
import br.com.goodware.service.SignatureService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.sql.*;
import java.time.Instant;
import java.util.*;

@Repository
@Transactional
public class AnalysisRepository {
 private final JdbcTemplate jdbc;
 private final ObjectMapper mapper;
 public AnalysisRepository(JdbcTemplate jdbc,ObjectMapper mapper){this.jdbc=jdbc;this.mapper=mapper;}
 private UUID scope(){
  UUID owner=br.com.goodware.auth.OwnerContext.require();
  Boolean postgres=jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>) c->c.getMetaData().getDatabaseProductName().equals("PostgreSQL"));
  if(Boolean.TRUE.equals(postgres)) {
   if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Transação obrigatória.");
   jdbc.queryForObject("SELECT set_config('goodware.owner_id',?,true)",String.class,owner.toString());
  }
  return owner;
 }
 private static UUID uuid(String id){return UUID.fromString(id);}
 public void start(String id,String name,String hash,long size,Instant now){
  jdbc.update("INSERT INTO goodware.analyses(id,name,sha256,upload_size,status,created_at,started_at,owner_id) VALUES (?,?,?,?,'RUNNING',?,?,?)",
   uuid(id),name,hash,size,Timestamp.from(now),Timestamp.from(now),scope());
 }
 @Transactional
 public void complete(AnalysisResult result){
  UUID owner=scope();
  UUID id=uuid(result.id());
  // Lock parent before children so deletion/completion cannot race.
  var states=jdbc.queryForList("SELECT status FROM goodware.analyses WHERE id=? AND owner_id=? FOR UPDATE",String.class,id,owner);
  if(states.size()!=1 || !states.getFirst().equals("RUNNING"))throw new IllegalStateException("Estado da análise incompatível.");
  for(FileReport f:result.files()) jdbc.update("INSERT INTO goodware.analyzed_files(analysis_id,path,size_bytes,sha256,language,status,note) VALUES (?,?,?,?,?,?,?)",
   id,f.path(),f.size(),f.sha256(),f.language(),f.status(),f.note());
  for(Finding f:result.findings()) jdbc.update("INSERT INTO goodware.findings(analysis_id,id,file_path,category,severity,confidence,title,line_start,line_end,rule_id,rule_version,evidence,impact,recommendation) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
   id,f.id(),"[pacote ZIP]".equals(f.file())?null:f.file(),f.category(),f.severity(),f.confidence(),f.title(),f.line(),f.line(),f.ruleId(),
   f.category().endsWith("SIGNATURE")?result.catalogVersion():result.engineVersion(),Evidence.redact(f.evidence()),f.impact(),f.recommendation());
  jdbc.update("UPDATE goodware.analyses SET status='COMPLETED',completed_at=?,warnings=?,engine_version=?,catalog_version=?,signature_count=?,duration_ms=? WHERE id=? AND owner_id=?",
   Timestamp.from(Instant.now()),json(result.warnings()),result.engineVersion(),result.catalogVersion(),result.signatureCount(),result.durationMs(),id,owner);
 }
 public void fail(String id){jdbc.update("UPDATE goodware.analyses SET status='FAILED',completed_at=?,failure_message=? WHERE id=? AND owner_id=? AND status='RUNNING'",
  Timestamp.from(Instant.now()),"Análise não concluída. Verifique o ZIP e tente novamente.",uuid(id),scope());}
 @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
 public Optional<AnalysisResult> find(String id){
  try {return jdbc.query("SELECT * FROM goodware.analyses WHERE id=? AND owner_id=?",this::read,uuid(id),scope()).stream().findFirst();}
  catch(IllegalArgumentException e){return Optional.empty();}
 }
 @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
 public Optional<AnalysisResult> byHash(String hash){return jdbc.query("SELECT * FROM goodware.analyses WHERE sha256=? AND owner_id=? ORDER BY created_at DESC,id DESC LIMIT 1",this::read,hash.toLowerCase(Locale.ROOT),scope()).stream().findFirst();}
 @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
 public List<AnalysisResult> list(int page,int size){return jdbc.query("SELECT * FROM goodware.analyses WHERE owner_id=? ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",this::read,scope(),size,(long)page*size);}
 public void recover(){jdbc.update("UPDATE goodware.analyses SET status='FAILED',completed_at=?,failure_message=? WHERE owner_id=? AND status='RUNNING' AND started_at<?",
  Timestamp.from(Instant.now()),"Processamento interrompido. Envie o ZIP novamente.",scope(),Timestamp.from(Instant.now().minusSeconds(600)));}
 public long count(){return jdbc.queryForObject("SELECT count(*) FROM goodware.analyses WHERE owner_id=?",Long.class,scope());}
 @Transactional
 public boolean delete(String id){
  try {return jdbc.update("DELETE FROM goodware.analyses WHERE id=? AND owner_id=? AND status<>'RUNNING'",uuid(id),scope())>0;}
  catch(IllegalArgumentException e){return false;}
 }
 public Map<String,Object> state(String id){
  try {return jdbc.query("SELECT status,created_at,started_at,completed_at,failure_message,upload_size FROM goodware.analyses WHERE id=? AND owner_id=?",(r,n)->{
   Map<String,Object> s=new LinkedHashMap<>();s.put("status",r.getString(1));s.put("createdAt",r.getTimestamp(2).toInstant());s.put("startedAt",r.getTimestamp(3).toInstant());
   s.put("completedAt",r.getTimestamp(4)==null?null:r.getTimestamp(4).toInstant());s.put("failureMessage",r.getString(5));s.put("uploadSize",r.getLong(6));return s;
  },uuid(id),scope()).stream().findFirst().orElse(Map.of());}catch(IllegalArgumentException e){return Map.of();}
 }
 public List<Map<String,Object>> summary(String id){return jdbc.queryForList("SELECT category,severity,count(*) AS total FROM goodware.findings WHERE analysis_id=? AND EXISTS (SELECT 1 FROM goodware.analyses a WHERE a.id=analysis_id AND a.owner_id=?) GROUP BY category,severity ORDER BY category,severity",uuid(id),scope());}
 @Transactional
 public void snapshot(SignatureService.Catalog catalog){
  for(var s:catalog.signatures()) {
   Integer exists=jdbc.queryForObject("SELECT count(*) FROM goodware.signature_catalog WHERE catalog_version=? AND id=?",Integer.class,catalog.version(),s.id());
   if(exists>0) {
    var saved=jdbc.queryForObject("SELECT * FROM goodware.signature_catalog WHERE catalog_version=? AND id=?",(r,n)->new SignatureService.Signature(r.getString("id"),r.getString("indicator_type"),r.getString("indicator_value"),r.getString("description"),r.getString("source"),r.getDate("updated_at").toString(),r.getString("confidence"),r.getString("classification"),r.getString("license")),catalog.version(),s.id());
    if(!s.equals(saved))throw new IllegalStateException("Catálogo alterado sem incrementar sua versão.");
   }
   if(exists==0)jdbc.update("INSERT INTO goodware.signature_catalog(id,catalog_version,indicator_type,indicator_value,description,source,updated_at,confidence,classification,license) VALUES (?,?,?,?,?,?,?,?,?,?)",
    s.id(),catalog.version(),s.type(),s.value(),s.description(),s.source(),java.sql.Date.valueOf(s.updatedAt()),s.confidence(),s.classification(),s.license());
  }
 }
 private AnalysisResult read(ResultSet r,int row)throws SQLException {
  UUID id=r.getObject("id",UUID.class);
  List<Finding> findings=jdbc.query("SELECT * FROM goodware.findings WHERE analysis_id=? ORDER BY file_path,line_start,id",(f,n)->new Finding(f.getString("id"),f.getString("category"),f.getString("severity"),f.getString("confidence"),f.getString("title"),
   f.getString("file_path")==null?"[pacote ZIP]":f.getString("file_path"),f.getObject("line_start",Integer.class),f.getString("rule_id"),f.getString("evidence"),f.getString("impact"),f.getString("recommendation")),id);
  List<FileReport> files=jdbc.query("SELECT * FROM goodware.analyzed_files WHERE analysis_id=? ORDER BY path",(f,n)->new FileReport(f.getString("path"),f.getLong("size_bytes"),f.getString("sha256"),f.getString("language"),f.getString("status"),f.getString("note"),
   (int)findings.stream().filter(x->x.file().equals(fUnchecked(f,"path"))).count()),id);
  return new AnalysisResult(id.toString(),r.getString("name"),r.getString("status"),r.getString("sha256"),r.getTimestamp("created_at").toInstant(),files.size(),
   count(findings,"CRITICAL"),count(findings,"HIGH"),count(findings,"MEDIUM"),count(findings,"LOW"),findings,files,warnings(r.getString("warnings")),r.getString("engine_version"),r.getString("catalog_version"),r.getInt("signature_count"),r.getLong("duration_ms"));
 }
 private static String fUnchecked(ResultSet r,String key){try{return r.getString(key);}catch(SQLException e){throw new IllegalStateException(e);}}
 private int count(List<Finding> fs,String s){return (int)fs.stream().filter(f->s.equals(f.severity())).count();}
 private String json(Object o){try{return mapper.writeValueAsString(o);}catch(Exception e){throw new IllegalStateException("Metadados inválidos.");}}
 private List<String> warnings(String s){try{return mapper.readValue(s,new TypeReference<List<String>>(){});}catch(Exception e){throw new IllegalStateException("Metadados inválidos.");}}
}

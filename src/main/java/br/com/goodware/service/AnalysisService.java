package br.com.goodware.service;
import br.com.goodware.engine.*;
import br.com.goodware.model.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.Semaphore;

@Service
public class AnalysisService {
 private static final int MAX_TEXT=2*1024*1024;
 private final br.com.goodware.persistence.AnalysisRepository repository;
 private final Semaphore gate=new Semaphore(1);
 private final StaticRuleService rules;
 private final SignatureService signatures;
 public AnalysisService(StaticRuleService rules,SignatureService signatures,br.com.goodware.persistence.AnalysisRepository repository){this.rules=rules;this.signatures=signatures;this.repository=repository;}
 public AnalysisResult analyze(MultipartFile multipart) throws IOException {
  if(!gate.tryAcquire())throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Uma análise está em andamento. Aguarde e tente novamente.");
  long started=System.nanoTime(),deadline=started+30_000_000_000L;
  String analysisId=null;
  Instant createdAt=Instant.now();
  try {
   if(multipart.isEmpty())throw new ScanException("Envie um ZIP não vazio.");
   if(multipart.getSize()>SafeZipReader.MAX_UPLOAD)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"O ZIP excede 100 MiB.");
   String name=Optional.ofNullable(multipart.getOriginalFilename()).orElse("upload.zip");
   if(!name.toLowerCase(Locale.ROOT).endsWith(".zip"))throw new ScanException("Envie um arquivo .zip.");
   byte[] archive;
   try(InputStream in=multipart.getInputStream()) {archive=in.readNBytes(SafeZipReader.MAX_UPLOAD+1);}
   if(archive.length>SafeZipReader.MAX_UPLOAD)throw new ScanException("O ZIP excede 100 MiB.");
   analysisId=UUID.randomUUID().toString();
   repository.start(analysisId,displayName(name),Hashes.sha256(archive),archive.length,createdAt);
   List<Finding> findings=new ArrayList<>(signatures.scanHash("[pacote ZIP]",Hashes.sha256(archive)));
   List<FileReport> files=new ArrayList<>();
   new SafeZipReader().read(archive,(path,bytes) -> {
    SafeZipReader.checkTime(deadline);
    String hash=Hashes.sha256(bytes),language=language(path),status="HASH_ONLY",note="Tipo sem regras textuais; verificação SHA-256 realizada.";
    int before=findings.size();findings.addAll(signatures.scanHash(path,hash));
    if(!language.equals("binary")) {
     if(bytes.length>MAX_TEXT)note="Texto acima de 2 MiB; apenas SHA-256.";
     else {
      try {
       String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
       if(text.indexOf('\0')>=0)note="Conteúdo binário detectado; apenas SHA-256.";
       else {
        findings.addAll(rules.scanText(path,language,text));findings.addAll(signatures.scanText(path,text));
        boolean longLines=text.lines().anyMatch(l -> l.length()>4096);
        status=longLines?"PARTIAL":"TEXT_SCANNED";
        note=longLines?"Linhas acima de 4096 caracteres omitidas nas regras por linha.":"Regras por extensão e indicadores aplicados; cobertura heurística limitada.";
       }
      } catch(CharacterCodingException ex) {note="Codificação não UTF-8; apenas SHA-256.";}
     }
    }
    if(findings.size()>1000)throw new ScanException("Limite de 1000 achados excedido. Divida o projeto.");
    files.add(new FileReport(path,bytes.length,hash,language,status,note,findings.size()-before));
    SafeZipReader.checkTime(deadline);
   },deadline);
   if(files.isEmpty())throw new ScanException("O ZIP contém somente diretórios.");
   List<String> warnings=new ArrayList<>(List.of("Nenhum achado não garante segurança. Heurísticas não comprovam exploração ou malware.",
    "Análise sem execução, sem consulta externa e sem resolver dependências transitivas. ZIPs internos não são abertos.",
    "Relatórios persistidos no banco PostgreSQL; exclusão manual pelo proprietário."));
   if(signatures.size()==0)warnings.add("Catálogo de ameaças vazio: nenhum hash ou endereço será classificado como ameaça conhecida.");
   if(files.stream().anyMatch(f -> !f.status().equals("TEXT_SCANNED")))warnings.add("Cobertura parcial: consulte o inventário de arquivos e os motivos.");
   warnings.add("Dependências: somente declarações explícitas de log4j-core em pom.xml e CVE-2021-44228; não é auditoria completa de componentes.");
   AnalysisResult result=new AnalysisResult(analysisId,displayName(name),"COMPLETED",Hashes.sha256(archive),createdAt,
    files.size(),count(findings,"CRITICAL"),count(findings,"HIGH"),count(findings,"MEDIUM"),count(findings,"LOW"),List.copyOf(findings),List.copyOf(files),List.copyOf(warnings),
    "1.0.0",signatures.version(),signatures.size(),(System.nanoTime()-started)/1_000_000);
   repository.complete(result);
   return result;
  } catch(IOException | RuntimeException ex) {
   if(analysisId!=null) {try {repository.fail(analysisId);}catch(RuntimeException persistenceFailure) {
    throw new org.springframework.dao.DataAccessResourceFailureException("Não foi possível registrar o resultado da análise.");
   }}
   throw ex;
  } finally {gate.release();}
 }
 private int count(List<Finding> f,String severity){return (int)f.stream().filter(x -> x.severity().equals(severity)).count();}
 public List<AnalysisResult> list(){repository.recover();return repository.list(0,30);}
 public Map<String,Object> history(int page,int size){
  if(page<0 || page>100000 || size<1 || size>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Página ou tamanho inválido (1 a 100).");
  repository.recover();
  return Map.of("items",repository.list(page,size),"page",page,"size",size,"total",repository.count());
 }
 public Map<String,Object> state(String id){var state=repository.state(id);if(state.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Análise não encontrada.");return state;}
 public List<Map<String,Object>> summary(String id){state(id);return repository.summary(id);}
 public Optional<AnalysisResult> findById(String id){return repository.find(id);}
 public Optional<AnalysisResult> findBySha256(String hash){return repository.byHash(hash);}
 public boolean delete(String id){
  var state=repository.state(id);
  if("RUNNING".equals(state.get("status")))throw new ResponseStatusException(HttpStatus.CONFLICT,"A análise ainda está em execução.");
  return repository.delete(id);
 }
 private String displayName(String name){String s=name.replaceAll("[\\p{Cntrl}\\p{Cf}/\\\\]","_");return s.substring(0,Math.min(s.length(),160));}
 public static String language(String path) {
  String name=path.substring(path.lastIndexOf('/')+1).toLowerCase(Locale.ROOT);
  if(name.equals(".env") || name.startsWith(".env.") || name.equals("dockerfile") || name.equals("requirements.txt"))return "config";
  String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1):"";
  return switch(ext) {
   case "java" -> "java";case "js","jsx","mjs","cjs" -> "javascript";case "ts","tsx" -> "typescript";
   case "py" -> "python";case "php" -> "php";case "rb" -> "ruby";case "go" -> "go";case "cs" -> "csharp";
   case "c","h","cpp","hpp" -> "cpp";case "sh","bat","cmd" -> "shell";case "ps1" -> "powershell";
   case "json","xml","yml","yaml","properties","env","conf","config","txt","sql","html","htm","pem","key" -> "config";
   default -> "binary";
  };
 }
}

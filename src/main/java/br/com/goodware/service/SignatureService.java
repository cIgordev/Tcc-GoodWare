package br.com.goodware.service;
import br.com.goodware.engine.*;
import br.com.goodware.model.Finding;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.io.*;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class SignatureService {
 public record Signature(String id,String type,String value,String description,String source,
   String updatedAt,String confidence,String classification,String license) {}
 public record Catalog(String version,List<Signature> signatures) {}
 private final Catalog catalog;
 private final Map<String,Signature> hashes=new HashMap<>();
 private final Map<String,Signature> hosts=new HashMap<>();
 private final Map<String,Signature> urls=new HashMap<>();
 private static final Pattern URL=Pattern.compile("(?i)https?://[^\\s\"'<>`]{1,2048}");
 @org.springframework.beans.factory.annotation.Autowired
 public SignatureService(ObjectMapper mapper) {
  this(mapper,SignatureService.class.getResourceAsStream("/signatures/catalog.json"));
 }
 public SignatureService(ObjectMapper mapper,InputStream input) {
  try(InputStream in=input) {
   catalog=mapper.readValue(in,Catalog.class);
   if(catalog.version()==null || catalog.version().isBlank() || catalog.signatures()==null || catalog.signatures().size()>10000) throw new IllegalArgumentException();
   Set<String> ids=new HashSet<>();
   for(Signature s:catalog.signatures()) {
    if(!ids.add(s.id()) || s.id()==null || !s.id().matches("[A-Za-z0-9_-]{1,80}") || s.description()==null || s.description().isBlank()
      || s.source()==null || !s.source().startsWith("https://") || s.license()==null || s.license().isBlank()
      || !Set.of("LOW","MEDIUM","HIGH").contains(s.confidence())
      || !Set.of("MALWARE","SUSPICIOUS","TEST").contains(s.classification())) throw new IllegalArgumentException();
    LocalDate.parse(s.updatedAt());
    String value=s.value();
    Map<String,Signature> index;
    switch(s.type()) {
     case "SHA256" -> {if(!value.matches("[a-fA-F0-9]{64}"))throw new IllegalArgumentException();value=value.toLowerCase(Locale.ROOT);index=hashes;}
     case "DOMAIN" -> {if(!value.matches("(?i)[a-z0-9](?:[a-z0-9.-]{0,251}[a-z0-9])?\\.[a-z]{2,63}"))throw new IllegalArgumentException();value=value.toLowerCase(Locale.ROOT);index=hosts;}
     case "IPV4" -> {String[] parts=value.split("\\.");if(parts.length!=4)throw new IllegalArgumentException();for(String p:parts)if(!p.matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(p)>255)throw new IllegalArgumentException();index=hosts;}
     case "URL" -> {URI u=URI.create(value);if(u.getHost()==null || !Set.of("http","https").contains(u.getScheme()))throw new IllegalArgumentException();index=urls;}
     default -> throw new IllegalArgumentException();
    }
    if(index.putIfAbsent(value,s)!=null)throw new IllegalArgumentException();
   }
  } catch(Exception ex) {throw new IllegalStateException("Catálogo local inválido. Revise os metadados de procedência.",ex);}
 }
 public Catalog catalog(){return catalog;}
 public String version(){return catalog.version();}
 public int size(){return catalog.signatures().size();}
 public List<Finding> scanHash(String path,String hash) {
  Signature s=hashes.get(hash.toLowerCase(Locale.ROOT));
  return s==null?List.of():List.of(finding(s,path,null,"SHA-256: "+hash));
 }
 public List<Finding> scanText(String path,String text) {
  if(hosts.isEmpty() && urls.isEmpty())return List.of();
  List<Finding> findings=new ArrayList<>();String[] lines=text.split("\\R",-1);
  for(int i=0;i<lines.length;i++) {
   if(lines[i].length()>4096)continue;
   var matcher=URL.matcher(lines[i]);Set<String> seen=new HashSet<>();
   while(matcher.find()) {
    String raw=matcher.group();
    try {
     URI uri=URI.create(raw);String host=uri.getHost();
     for(Signature s:new Signature[]{urls.get(raw),host==null?null:hosts.get(host.toLowerCase(Locale.ROOT))}) {
      if(s!=null && seen.add(s.id()))findings.add(finding(s,path,i+1,"Indicador catalogado: "+s.value()));
     }
    } catch(IllegalArgumentException ignored) { /* URL não interpretável: não visitar. */ }
   }
   if(findings.size()>300)throw new ScanException("Mais de 300 indicadores em um arquivo.");
  }
  return findings;
 }
 private Finding finding(Signature s,String path,Integer line,String evidence) {
  boolean test=s.classification().equals("TEST");
  return Evidence.finding("GW-SIG-"+s.id(),test?"TEST_SIGNATURE":"KNOWN_SIGNATURE",test?"LOW":"HIGH",s.confidence(),
   test?"Correspondência de teste (não é malware)":"Correspondência exata com indicador catalogado",path,line,
   evidence+" | "+s.description()+" | "+s.classification(),
   "Correspondência com o catálogo, sem execução. Fonte: "+s.source()+"; atualização: "+s.updatedAt()+". Um indicador de rede isolado não comprova comprometimento.",
   "Valide a procedência, data e contexto do indicador antes de concluir. Não execute nem visite o conteúdo suspeito.");
 }
}

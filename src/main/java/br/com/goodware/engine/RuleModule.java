package br.com.goodware.engine;
import br.com.goodware.model.Finding;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

/** Heurísticas por linha: não infere fluxo de dados nem comprova exploração. */
public class RuleModule implements ScanModule {
 public record Rule(String id,List<String> languages,String category,String severity,String confidence,
  String title,String pattern,String impact,String recommendation) {}
 private record Compiled(Rule rule,Pattern pattern) {}
 private final List<Compiled> rules;
 public RuleModule(ObjectMapper mapper,String resource) {
  try(InputStream in=getClass().getResourceAsStream(resource)) {
   Rule[] loaded=mapper.readValue(in,Rule[].class);
   rules=Arrays.stream(loaded).map(r -> new Compiled(r,Pattern.compile(r.pattern()))).toList();
  } catch(Exception ex) {throw new IllegalStateException("Catálogo de regras inválido: "+resource,ex);}
 }
 @Override public List<Finding> scan(String path,String language,String content) {
  List<Finding> found=new ArrayList<>();
  String[] lines=content.split("\\R",-1);
  boolean block=false;
  for(int i=0;i<lines.length;i++) {
   String original=lines[i];
   // Linhas extensas são identificadas como cobertura parcial pelo serviço.
   if(original.length()>4096) continue;
   StringBuilder code=new StringBuilder(); char quote=0; boolean escape=false;
   for(int c=0;c<original.length();c++) {
    char ch=original.charAt(c), next=c+1<original.length()?original.charAt(c+1):0;
    if(block) {if(ch=='*' && next=='/') {block=false;c++;} continue;}
    if(quote!=0) {code.append(ch);if(escape)escape=false;else if(ch=='\\')escape=true;else if(ch==quote)quote=0;continue;}
    if(ch=='\'' || ch=='"' || ch=='`') {quote=ch;code.append(ch);continue;}
    boolean cStyle=Set.of("java","javascript","typescript","php","csharp","go","cpp").contains(language);
    if(cStyle && ch=='/' && next=='*') {block=true;c++;continue;}
    if(cStyle && ch=='/' && next=='/') break;
    if(Set.of("python","shell","powershell","config","ruby").contains(language) && ch=='#') break;
    code.append(ch);
   }
   String line=code.toString();
   if(line.isBlank())continue;
   for(Compiled compiled:rules) {
    Rule r=compiled.rule();
    if(!r.languages().contains("*") && !r.languages().contains(language))continue;
    if(r.id().contains("SECRET") && placeholder(line))continue;
    if(r.id().equals("GW-PY-DESERIALIZE-001") && line.contains("yaml.load") && (line.contains("SafeLoader") || line.contains("CSafeLoader")))continue;
    if(compiled.pattern().matcher(line).find()) {
     String evidence=r.id().contains("SECRET") ? "Atribuição de possível credencial literal [REDACTED]; linha "+(i+1) : original;
     found.add(Evidence.finding(r.id(),r.category(),r.severity(),r.confidence(),r.title(),path,i+1,evidence,r.impact(),r.recommendation()));
     if(found.size()>300) throw new ScanException("Mais de 300 achados em um arquivo. Divida o projeto.");
    }
   }
  }
  return found;
 }
 private boolean placeholder(String line) {
  String value=line.substring(Math.max(line.indexOf('='),line.indexOf(':'))+1).strip().replaceAll("^[\"']|[\"';,]+$","");
  return value.matches("(?i)(?:\\$\\{[^}]+}|process\\.env\\.[A-Z_]+|os\\.getenv.*|System\\.getenv.*|changeme|change_me|your[_-].*|example|placeholder|<[^>]+>)");
 }
}

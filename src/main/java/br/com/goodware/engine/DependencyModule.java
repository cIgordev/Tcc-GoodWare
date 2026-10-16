package br.com.goodware.engine;
import br.com.goodware.model.Finding;
import java.io.StringReader;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/** Apenas declarações explícitas de log4j-core em pom.xml; nenhum Maven do upload é executado. */
public final class DependencyModule implements ScanModule {
 @Override public List<Finding> scan(String path,String language,String content) {
  if(!path.equals("pom.xml") && !path.endsWith("/pom.xml")) return List.of();
  try {
   DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
   f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
   f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
   f.setFeature("http://xml.org/sax/features/external-general-entities",false);
   f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
   f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,""); f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
   f.setXIncludeAware(false);f.setExpandEntityReferences(false);
   var builder=f.newDocumentBuilder();builder.setErrorHandler(new DefaultHandler());
   Document document=builder.parse(new InputSource(new StringReader(content)));
   List<Finding> findings=new ArrayList<>();
   NodeList dependencies=document.getElementsByTagName("dependency");
   for(int i=0;i<dependencies.getLength();i++) {
    Element d=(Element)dependencies.item(i);
    if(!text(d,"groupId").equals("org.apache.logging.log4j") || !text(d,"artifactId").equals("log4j-core"))continue;
    String version=text(d,"version");
    if(version.length()>80)version="[versão extensa]";
    if(affected(version)) {
     findings.add(Evidence.finding("GW-DEP-CVE-2021-44228","DEPENDENCY_ADVISORY","CRITICAL","HIGH",
      "log4j-core declarado em faixa afetada por CVE-2021-44228",path,null,
      "org.apache.logging.log4j:log4j-core:"+version+"; fonte: https://logging.apache.org/security.html; revisão: 2026-10-14",
      "O componente declarado está em faixa afetada por Log4Shell. A declaração não comprova presença em execução nem exploração.",
      "Atualize para uma versão mantida e revise TODOS os avisos atuais do fornecedor. Confirme a versão resolvida no seu ambiente confiável."));
    } else if(!version.matches("\\d{1,3}\\.\\d{1,3}(?:\\.\\d{1,3})?")) {
     findings.add(Evidence.finding("GW-DEP-UNRESOLVED","SUSPICIOUS_INDICATOR","LOW","LOW",
      "Versão de log4j-core não resolvida",path,null,"Dependência declarada; versão literal não suportada: "+version,
      "Propriedades, intervalos, herança e pré-lançamentos não são resolvidos por este MVP.",
      "Verifique a árvore de dependências no seu ambiente confiável e consulte o fornecedor."));
    }
   }
   return findings.stream().distinct().toList();
  } catch(Exception ex) {
   return List.of(Evidence.finding("GW-DEP-UNREADABLE","COVERAGE_NOTICE","LOW","HIGH",
    "Manifesto Maven não pôde ser interpretado",path,null,"XML inválido, DTD não permitido ou estrutura excedida.",
    "A avaliação de dependências deste manifesto ficou incompleta.","Revise o XML sem habilitar entidades externas; não execute o projeto para analisá-lo."));
  }
 }
 private static String text(Element e,String name) {
  for(Node n=e.getFirstChild();n!=null;n=n.getNextSibling())if(n instanceof Element c && c.getTagName().equals(name))return c.getTextContent().trim();
  return "";
 }
 static boolean affected(String version) {
  if(!version.matches("\\d{1,3}\\.\\d{1,3}(?:\\.\\d{1,3})?"))return false;
  int[] v=Arrays.stream(version.split("\\.")).mapToInt(Integer::parseInt).toArray();
  int major=v[0],minor=v[1],patch=v.length>2?v[2]:0;
  return major==2 && (minor<3 || minor==3 && patch<1 || minor>=4 && minor<12 || minor==12 && patch<2 || minor>=13 && minor<15);
 }
}

package br.com.goodware.service;
import br.com.goodware.engine.*;
import br.com.goodware.model.Finding;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class StaticRuleService {
 private final List<ScanModule> modules;
 public StaticRuleService(ObjectMapper mapper) {
  modules=List.of(new RuleModule(mapper,"/rules/vulnerabilities.json"),new RuleModule(mapper,"/rules/indicators.json"),new DependencyModule());
 }
 public List<Finding> scanText(String file,String language,String content) {
  return modules.stream().flatMap(m -> m.scan(file,language,content).stream()).toList();
 }
}

package br.com.goodware.engine;
import br.com.goodware.service.StaticRuleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RuleModuleTest {
 private final StaticRuleService rules=new StaticRuleService(new ObjectMapper());
 @Test void detectsExecWithLineAndEvidence(){var f=rules.scanText("A.java","java","class A {\n Runtime.getRuntime().exec(input);\n}");assertEquals(1,f.size());assertEquals(2,f.getFirst().line());assertEquals("GW-JAVA-EXEC-001",f.getFirst().ruleId());assertTrue(f.getFirst().evidence().contains("exec(input)"));}
 @Test void ignoresCommentsAndWrongLanguage(){assertTrue(rules.scanText("A.java","java","// Runtime.getRuntime().exec(input);\n/*\n Runtime.getRuntime().exec(input);\n*/").isEmpty());assertTrue(rules.scanText("readme.txt","config","Runtime.getRuntime().exec(input);").isEmpty());}
 @Test void distinguishesPreparedSql(){assertTrue(rules.scanText("A.java","java","connection.prepareStatement(\"SELECT * FROM x WHERE id = ?\");").isEmpty());assertFalse(rules.scanText("A.java","java","statement.executeQuery(\"SELECT * FROM x WHERE id = \" + id);").isEmpty());}
 @Test void masksQuotedJsonSecretsAndOtherRules(){var f=rules.scanText("a.json","config","{\"password\": \"very secret 12345\"}");assertEquals(1,f.size());assertFalse(f.toString().contains("very secret"));assertTrue(f.getFirst().evidence().contains("REDACTED"));assertFalse(Evidence.redact("eval(x); password = \"secret value\"").contains("secret value"));}
 @Test void placeholdersAreNotSecrets(){assertTrue(rules.scanText(".env","config","PASSWORD=${MY_PASSWORD}\nTOKEN=your_token_here").isEmpty());assertTrue(rules.scanText("a.js","javascript","const password = process.env.PASSWORD;").isEmpty());}
 @Test void uniqueIdsAcrossFiles(){var a=rules.scanText("A.java","java","Runtime.getRuntime().exec(x);").getFirst();var b=rules.scanText("B.java","java","Runtime.getRuntime().exec(x);").getFirst();assertNotEquals(a.id(),b.id());}
 @Test void separatesObfuscationAndVulnerability(){var f=rules.scanText("a.js","javascript","eval(atob(payload));");assertTrue(f.stream().anyMatch(x->x.category().equals("SUSPICIOUS_INDICATOR")));assertTrue(f.stream().anyMatch(x->x.category().equals("POTENTIAL_VULNERABILITY")));}
 @Test void deserializationAndSafeLoader(){assertFalse(rules.scanText("a.py","python","pickle.loads(data)").isEmpty());assertTrue(rules.scanText("a.py","python","yaml.load(data, Loader=yaml.SafeLoader)").isEmpty());}
 @Test void tlsAndPrivateKey(){assertFalse(rules.scanText("a.py","python","requests.get(url, verify=False)").isEmpty());assertFalse(rules.scanText("a.pem","config","-----BEGIN PRIVATE KEY-----").isEmpty());}
 @Test void boundsFindings(){assertThrows(ScanException.class,()->rules.scanText("a.java","java","Runtime.getRuntime().exec(x);\n".repeat(400)));}
 @Test void sqlLikeStringIsKnownHeuristicLimitation(){assertFalse(rules.scanText("a.js","javascript","const documentation = \"eval(x)\";").isEmpty());}
 @Test void regexExecIsNotShellExec(){assertTrue(rules.scanText("a.js","javascript","const match = pattern.exec(input);").isEmpty());assertFalse(rules.scanText("a.js","javascript","child_process.exec(input);").isEmpty());assertFalse(rules.scanText("a.js","javascript","execSync(input);").isEmpty());}
}

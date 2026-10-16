package br.com.goodware.engine;
import br.com.goodware.model.Finding;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
public final class Evidence {
 private Evidence() {}
 private static final Pattern SECRET = Pattern.compile("(?i)([\"']?(?:api[_-]?key|secret|password|passwd|token|access[_-]?key)[\"']?\\s*[:=]\\s*).*");
 public static String redact(String text) {
  String clean = SECRET.matcher(text.strip()).replaceAll("$1[REDACTED]");
  clean = clean.replaceAll("(?i)Bearer\\s+[^\\s]+", "Bearer [REDACTED]");
  clean = clean.replaceAll("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+", "[REDACTED JWT]");
  // Minimize literals: retain code structure, not embedded credentials or payloads.
  clean = clean.replaceAll("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'", "[REDACTED]");
  clean = clean.replaceAll("(?i)(https?://)[^/\\s:@]+:[^/\\s@]+@", "$1[REDACTED]@");
  return clean.length() > 260 ? clean.substring(0,260) + "…" : clean;
 }
 public static Finding finding(String rule, String category, String severity, String confidence,
    String title, String path, Integer line, String evidence, String impact, String recommendation) {
  String key = rule + "\n" + path + "\n" + line + "\n" + evidence;
  return new Finding(rule + "-" + Hashes.sha256(key.getBytes(StandardCharsets.UTF_8)).substring(0,16),
   category, severity, confidence, title, path, line, rule, redact(evidence), impact, recommendation);
 }
}

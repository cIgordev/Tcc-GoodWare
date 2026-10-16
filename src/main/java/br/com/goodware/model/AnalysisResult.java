package br.com.goodware.model;
import java.time.Instant;
import java.util.List;
public record AnalysisResult(String id, String name, String status, String sha256,
 Instant createdAt, int filesAnalyzed, int critical, int high, int medium, int low,
 List<Finding> findings, List<FileReport> files, List<String> warnings,
 String engineVersion, String catalogVersion, int signatureCount, long durationMs) {}

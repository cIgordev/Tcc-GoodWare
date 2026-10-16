package br.com.goodware.model;

public record Finding(
        String id,
        String category,
        String severity,
        String confidence,
        String title,
        String file,
        Integer line,
        String ruleId,
        String evidence,
        String impact,
        String recommendation
) {}

package br.com.goodware.model;
public record FileReport(String path, long size, String sha256, String language,
 String status, String note, int findings) {}

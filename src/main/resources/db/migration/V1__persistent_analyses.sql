CREATE SCHEMA IF NOT EXISTS goodware;
CREATE TABLE goodware.analyses (
 id UUID PRIMARY KEY,
 name VARCHAR(160) NOT NULL,
 sha256 VARCHAR(64) NOT NULL CHECK (sha256 ~ '^[a-f0-9]{64}$'),
 upload_size BIGINT NOT NULL CHECK (upload_size BETWEEN 1 AND 104857600),
 status VARCHAR(16) NOT NULL CHECK (status IN ('RUNNING','COMPLETED','FAILED')),
 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 started_at TIMESTAMP WITH TIME ZONE NOT NULL,
 completed_at TIMESTAMP WITH TIME ZONE,
 failure_message VARCHAR(240),
 warnings TEXT NOT NULL DEFAULT '[]',
 engine_version VARCHAR(40), catalog_version VARCHAR(80),
 signature_count INTEGER NOT NULL DEFAULT 0 CHECK (signature_count >= 0),
 duration_ms BIGINT NOT NULL DEFAULT 0 CHECK (duration_ms >= 0),
 CHECK (completed_at IS NULL OR completed_at >= started_at),
 CHECK ((status = 'RUNNING' AND completed_at IS NULL AND failure_message IS NULL)
     OR (status = 'COMPLETED' AND completed_at IS NOT NULL AND failure_message IS NULL)
     OR (status = 'FAILED' AND completed_at IS NOT NULL AND failure_message IS NOT NULL))
);
CREATE INDEX analyses_history_idx ON goodware.analyses(created_at DESC,id DESC);
CREATE INDEX analyses_sha256_idx ON goodware.analyses(sha256,created_at DESC);
CREATE TABLE goodware.analyzed_files (
 analysis_id UUID NOT NULL REFERENCES goodware.analyses(id) ON DELETE CASCADE,
 path VARCHAR(1024) NOT NULL,
 size_bytes BIGINT NOT NULL CHECK (size_bytes >= 0),
 sha256 VARCHAR(64) NOT NULL CHECK (sha256 ~ '^[a-f0-9]{64}$'),
 language VARCHAR(32) NOT NULL,
 status VARCHAR(24) NOT NULL CHECK (status IN ('HASH_ONLY','TEXT_SCANNED','PARTIAL')),
 note TEXT NOT NULL,
 PRIMARY KEY (analysis_id,path),
 CHECK (length(path) > 0 AND path NOT LIKE '/%' AND path NOT LIKE '%\%' AND path NOT LIKE '%:%'
   AND path <> '..' AND path NOT LIKE '../%' AND path NOT LIKE '%/../%' AND path NOT LIKE '%/..')
);
CREATE TABLE goodware.findings (
 analysis_id UUID NOT NULL REFERENCES goodware.analyses(id) ON DELETE CASCADE,
 id VARCHAR(160) NOT NULL,
 file_path VARCHAR(1024),
 category VARCHAR(40) NOT NULL CHECK (category IN ('POTENTIAL_VULNERABILITY','SUSPICIOUS_INDICATOR','KNOWN_SIGNATURE','TEST_SIGNATURE','DEPENDENCY_ADVISORY','COVERAGE_NOTICE')),
 severity VARCHAR(12) NOT NULL CHECK (severity IN ('CRITICAL','HIGH','MEDIUM','LOW','INFO')),
 confidence VARCHAR(12) NOT NULL CHECK (confidence IN ('HIGH','MEDIUM','LOW')),
 title TEXT NOT NULL,
 line_start INTEGER CHECK (line_start > 0),
 line_end INTEGER CHECK (line_end >= line_start),
 rule_id VARCHAR(160) NOT NULL,
 rule_version VARCHAR(80),
 evidence TEXT NOT NULL,
 impact TEXT NOT NULL,
 recommendation TEXT NOT NULL,
 PRIMARY KEY (analysis_id,id),
 FOREIGN KEY (analysis_id,file_path) REFERENCES goodware.analyzed_files(analysis_id,path) ON DELETE CASCADE
);
CREATE INDEX findings_file_idx ON goodware.findings(analysis_id,file_path);
CREATE INDEX findings_summary_idx ON goodware.findings(analysis_id,category,severity);
CREATE TABLE goodware.signature_catalog (
 id VARCHAR(80) NOT NULL,
 catalog_version VARCHAR(80) NOT NULL,
 indicator_type VARCHAR(12) NOT NULL CHECK (indicator_type IN ('SHA256','DOMAIN','IPV4','URL')),
 indicator_value TEXT NOT NULL,
 description TEXT NOT NULL,
 source TEXT NOT NULL CHECK (source LIKE 'https://%'),
 updated_at DATE NOT NULL,
 confidence VARCHAR(12) NOT NULL CHECK (confidence IN ('HIGH','MEDIUM','LOW')),
 classification VARCHAR(16) NOT NULL CHECK (classification IN ('MALWARE','SUSPICIOUS','TEST')),
 license TEXT NOT NULL,
 PRIMARY KEY (catalog_version,id),
 CHECK (indicator_type <> 'SHA256' OR indicator_value ~ '^[a-fA-F0-9]{64}$')
);

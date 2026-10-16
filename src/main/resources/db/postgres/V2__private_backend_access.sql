-- Run as migration administrator. Runtime login is provisioned separately, never embedded here.
DO $$ BEGIN
 IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='goodware_backend') THEN
   CREATE ROLE goodware_backend NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS;
 END IF;
END $$;
REVOKE ALL ON SCHEMA goodware FROM PUBLIC;
REVOKE ALL ON ALL TABLES IN SCHEMA goodware FROM PUBLIC;
DO $$ DECLARE r text; BEGIN
 FOREACH r IN ARRAY ARRAY['anon','authenticated','service_role'] LOOP
  IF EXISTS (SELECT FROM pg_roles WHERE rolname=r) THEN
   EXECUTE format('REVOKE ALL ON SCHEMA goodware FROM %I',r);
   EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA goodware FROM %I',r);
  END IF;
 END LOOP;
END $$;
GRANT USAGE ON SCHEMA goodware TO goodware_backend;
GRANT SELECT,INSERT,UPDATE,DELETE ON goodware.analyses,goodware.analyzed_files,goodware.findings TO goodware_backend;
GRANT SELECT,INSERT ON goodware.signature_catalog TO goodware_backend;
ALTER TABLE goodware.analyses ENABLE ROW LEVEL SECURITY;
ALTER TABLE goodware.analyzed_files ENABLE ROW LEVEL SECURITY;
ALTER TABLE goodware.findings ENABLE ROW LEVEL SECURITY;
ALTER TABLE goodware.signature_catalog ENABLE ROW LEVEL SECURITY;
-- Single-user backend role only. No browser/Data API role receives a policy or grant.
CREATE POLICY backend_analyses ON goodware.analyses TO goodware_backend USING (true) WITH CHECK (true);
CREATE POLICY backend_files ON goodware.analyzed_files TO goodware_backend USING (true) WITH CHECK (true);
CREATE POLICY backend_findings ON goodware.findings TO goodware_backend USING (true) WITH CHECK (true);
CREATE POLICY backend_catalog_read ON goodware.signature_catalog FOR SELECT TO goodware_backend USING (true);
CREATE POLICY backend_catalog_insert ON goodware.signature_catalog FOR INSERT TO goodware_backend WITH CHECK (true);

-- Non-destructive: no legacy ownership inference, no account/data deletion.
ALTER TABLE goodware.analyses ADD CONSTRAINT analyses_owner_fk FOREIGN KEY(owner_id) REFERENCES auth.users(id);
ALTER POLICY backend_analyses ON goodware.analyses
 USING (owner_id = (SELECT nullif(current_setting('goodware.owner_id',true),'')::uuid))
 WITH CHECK (owner_id = (SELECT nullif(current_setting('goodware.owner_id',true),'')::uuid));
ALTER POLICY backend_files ON goodware.analyzed_files
 USING (EXISTS (SELECT 1 FROM goodware.analyses a WHERE a.id=analysis_id))
 WITH CHECK (EXISTS (SELECT 1 FROM goodware.analyses a WHERE a.id=analysis_id));
ALTER POLICY backend_findings ON goodware.findings
 USING (EXISTS (SELECT 1 FROM goodware.analyses a WHERE a.id=analysis_id))
 WITH CHECK (EXISTS (SELECT 1 FROM goodware.analyses a WHERE a.id=analysis_id));
-- Legacy NULL owners may remain, but runtime cannot create one (WITH CHECK).

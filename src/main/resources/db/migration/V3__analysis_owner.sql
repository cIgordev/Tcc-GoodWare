-- Existing rows remain unassigned and invisible to application users.
ALTER TABLE goodware.analyses ADD COLUMN owner_id UUID;
CREATE INDEX analyses_owner_history_idx ON goodware.analyses(owner_id,created_at DESC,id DESC);
CREATE INDEX analyses_owner_hash_idx ON goodware.analyses(owner_id,sha256,created_at DESC);

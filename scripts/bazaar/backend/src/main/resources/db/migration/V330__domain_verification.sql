-- A custom domain is served only once the store proved it owns it: the
-- token it publishes (a file under /.well-known/bazaar-verify/ or a DNS TXT
-- record) and when that was checked. Changing the domain clears both.
ALTER TABLE store ADD COLUMN domain_token VARCHAR(64) NOT NULL DEFAULT '';
ALTER TABLE store ADD COLUMN domain_verified_at BIGINT NOT NULL DEFAULT 0;


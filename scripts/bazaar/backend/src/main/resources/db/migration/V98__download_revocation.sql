-- A download grant can be revoked (by the merchant, or by a refund of its
-- line) without losing its history: revoked_at is when, 0 while it works.
ALTER TABLE download_grant ADD COLUMN revoked_at BIGINT NOT NULL DEFAULT 0;
-- A licence key is sold per unit, so it names the order line it went to.
ALTER TABLE license_key ADD COLUMN line_id VARCHAR(32) NOT NULL DEFAULT '';
-- The same key is never pasted into one pool twice.
CREATE UNIQUE INDEX license_key_text ON license_key (variant_id, key_text);

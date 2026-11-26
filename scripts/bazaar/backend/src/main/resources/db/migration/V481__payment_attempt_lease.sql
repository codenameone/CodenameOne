-- When the reconciler claimed a payment attempt (RECONCILING). A claim older
-- than the lease belongs to a reconciler that stopped part way, and the next
-- pass takes the row over. 0 for a row never claimed.
ALTER TABLE payment_attempt ADD COLUMN claimed_at BIGINT NOT NULL DEFAULT 0;

-- The card token is no longer kept: a hold a crash orphaned is found at the
-- processor by its idempotency key. Clear any token an earlier version wrote.
UPDATE payment_attempt SET token = '';

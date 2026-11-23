-- A refund at the processor is claimed with what it asks for and when, so a
-- claim left behind by a crash can be finished later with the same
-- idempotency key instead of staying REFUNDING for good.
ALTER TABLE payment ADD COLUMN pending_refund_cents BIGINT NOT NULL DEFAULT 0;
ALTER TABLE payment ADD COLUMN refund_claimed_at BIGINT NOT NULL DEFAULT 0;

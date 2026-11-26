-- The IANA zone a store's analytics and reports count days and hours in.
ALTER TABLE store ADD COLUMN time_zone VARCHAR(64) NOT NULL DEFAULT 'UTC';

-- A checkout's idempotency key while it lives: the fingerprint of the request
-- that first used it, whether that request is still running, and the order it
-- placed. A row is forgotten once expires_at passes.
CREATE TABLE idempotency_key (
    client_id VARCHAR(64) NOT NULL PRIMARY KEY,
    fingerprint VARCHAR(64) NOT NULL,
    -- IN_FLIGHT while the first request runs, DONE once it placed its order.
    state VARCHAR(16) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL
);
CREATE INDEX idempotency_key_expires ON idempotency_key (expires_at);

-- A card hold checkout asked the processor for, written before the request
-- leaves and removed once the order records it: a row that outlives its
-- checkout is a hold a crash may have orphaned.
CREATE TABLE payment_attempt (
    order_id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    idem_key VARCHAR(190) NOT NULL,
    customer VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    -- STARTED, then RECONCILING while the reconciler settles it.
    status VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX payment_attempt_status ON payment_attempt (status, created_at);

-- One day of the processor's payments held against the orders' payments.
CREATE TABLE payment_reconciliation (
    report_day VARCHAR(10) NOT NULL PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    matched INT NOT NULL,
    provider_only INT NOT NULL,
    orders_only INT NOT NULL,
    mismatched INT NOT NULL,
    details TEXT NOT NULL,
    created_at BIGINT NOT NULL
);

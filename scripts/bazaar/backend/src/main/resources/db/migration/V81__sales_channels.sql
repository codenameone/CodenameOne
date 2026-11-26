-- Live marketplace sync: a store's connected marketplace accounts.
-- refresh_token and access_token are the marketplace's OAuth tokens for the
-- seller; no API ever answers them. webhook_secret signs the marketplace's
-- (or a relay's) order notifications.
CREATE TABLE sales_channel (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(16) NOT NULL,
    refresh_token TEXT NOT NULL,
    access_token TEXT NOT NULL,
    access_expires_at BIGINT NOT NULL,
    webhook_secret VARCHAR(128) NOT NULL,
    last_pull_at BIGINT NOT NULL,
    pull_requested SMALLINT NOT NULL,
    fail_count INTEGER NOT NULL,
    next_attempt_at BIGINT NOT NULL,
    last_error VARCHAR(500) NOT NULL,
    ledger_cursor BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX sales_channel_store ON sales_channel (store_id);

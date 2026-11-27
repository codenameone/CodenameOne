-- A variant listed on a channel and what was last pushed for it. version
-- counts pushes (each push's idempotency key is id:version); pushed_state
-- is the quantity and price last accepted, so an unchanged listing is never
-- pushed twice; seen_state and seen_at debounce a burst of changes.
CREATE TABLE channel_listing (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    channel_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    external_sku VARCHAR(64) NOT NULL,
    external_id VARCHAR(64) NOT NULL,
    etag VARCHAR(128) NOT NULL,
    version BIGINT NOT NULL,
    pushed_state VARCHAR(64) NOT NULL,
    pushed_qty INTEGER NOT NULL,
    pushed_price_cents BIGINT NOT NULL,
    seen_state VARCHAR(64) NOT NULL,
    seen_at BIGINT NOT NULL,
    urgent SMALLINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    fail_count INTEGER NOT NULL,
    next_attempt_at BIGINT NOT NULL,
    last_error VARCHAR(500) NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX channel_listing_store ON channel_listing (store_id);
CREATE UNIQUE INDEX channel_listing_variant ON channel_listing (channel_id, variant_id);
CREATE UNIQUE INDEX channel_listing_sku ON channel_listing (channel_id, external_sku);

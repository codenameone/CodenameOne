-- A marketplace order made into a Bazaar order once: (channel_id,
-- external_id) is unique, as is the order's client_id derived from it, so a
-- replayed pull or notification never makes a second order. lines_json keeps
-- the marketplace's line ids, which the shipment push names.
CREATE TABLE channel_order (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    channel_id VARCHAR(32) NOT NULL,
    external_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    lines_json TEXT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX channel_order_external ON channel_order (channel_id, external_id);
CREATE INDEX channel_order_order ON channel_order (order_id);
-- A shipment of a marketplace order told back to the marketplace.
CREATE TABLE channel_fulfillment (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    channel_id VARCHAR(32) NOT NULL,
    fulfillment_id VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    fail_count INTEGER NOT NULL,
    next_attempt_at BIGINT NOT NULL,
    last_error VARCHAR(500) NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX channel_fulfillment_key ON channel_fulfillment (fulfillment_id);

-- Serial and lot tracking. A variant's mode is a row of its own (none when
-- absent); every serial received is a tracked_unit, every lot a stock_lot
-- whose remaining units go first-expiring-first-out.
CREATE TABLE variant_tracking (
    variant_id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    mode VARCHAR(8) NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX variant_tracking_store ON variant_tracking (store_id);
CREATE TABLE stock_lot (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    location_id VARCHAR(32) NOT NULL,
    lot_code VARCHAR(64) NOT NULL,
    expires_at BIGINT NOT NULL,
    received_qty INTEGER NOT NULL,
    remaining INTEGER NOT NULL,
    reference VARCHAR(64) NOT NULL,
    received_at BIGINT NOT NULL
);
CREATE INDEX stock_lot_variant ON stock_lot (variant_id, location_id);
CREATE INDEX stock_lot_store ON stock_lot (store_id);
CREATE TABLE tracked_unit (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    location_id VARCHAR(32) NOT NULL,
    serial VARCHAR(64) NOT NULL,
    status VARCHAR(12) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    order_line_id VARCHAR(32) NOT NULL,
    reference VARCHAR(64) NOT NULL,
    received_at BIGINT NOT NULL,
    sold_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX tracked_unit_serial ON tracked_unit (store_id, serial);
CREATE INDEX tracked_unit_order ON tracked_unit (order_id);
CREATE TABLE lot_pick (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    lot_id VARCHAR(32) NOT NULL,
    reference VARCHAR(64) NOT NULL,
    quantity INTEGER NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX lot_pick_reference ON lot_pick (reference);

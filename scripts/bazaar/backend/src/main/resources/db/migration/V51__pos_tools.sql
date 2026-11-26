-- Wave B: tips, commissions, manager approvals, parked carts and the smart grid.
ALTER TABLE store_order ADD COLUMN tip_cents BIGINT NOT NULL DEFAULT 0;
CREATE TABLE pos_policy (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    discount_approval_bp BIGINT NOT NULL,
    approve_price_overrides SMALLINT NOT NULL,
    approve_refunds SMALLINT NOT NULL,
    approve_order_edits SMALLINT NOT NULL,
    tip_presets VARCHAR(80) NOT NULL,
    layaway_deposit_bp BIGINT NOT NULL,
    layaway_cancel_fee_bp BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE TABLE staff_rate (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    rate_bp BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE TABLE pos_approval (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    register_id VARCHAR(32) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    approver VARCHAR(190) NOT NULL,
    requested_by VARCHAR(190) NOT NULL,
    amount_cents BIGINT NOT NULL,
    reason VARCHAR(200) NOT NULL,
    used_by VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE TABLE parked_cart (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    register_id VARCHAR(32) NOT NULL,
    client_id VARCHAR(64) NOT NULL,
    label VARCHAR(80) NOT NULL,
    customer_id VARCHAR(32) NOT NULL,
    staff_username VARCHAR(190) NOT NULL,
    status VARCHAR(16) NOT NULL,
    note VARCHAR(500) NOT NULL,
    created_at BIGINT NOT NULL,
    resumed_at BIGINT NOT NULL
);
CREATE TABLE parked_line (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    parked_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    title VARCHAR(200) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_cents BIGINT NOT NULL,
    taxable SMALLINT NOT NULL,
    overridden SMALLINT NOT NULL,
    position INTEGER NOT NULL
);
CREATE TABLE grid_tile (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    location_id VARCHAR(32) NOT NULL,
    position INTEGER NOT NULL,
    kind VARCHAR(16) NOT NULL,
    ref VARCHAR(64) NOT NULL,
    label VARCHAR(80) NOT NULL,
    color INTEGER NOT NULL
);
CREATE UNIQUE INDEX pos_policy_store_id ON pos_policy (store_id);
CREATE UNIQUE INDEX staff_rate_store_id_username ON staff_rate (store_id, username);
CREATE INDEX pos_approval_store_id ON pos_approval (store_id);
CREATE UNIQUE INDEX parked_cart_register_id_client_id ON parked_cart (register_id, client_id);
CREATE INDEX parked_cart_store_id ON parked_cart (store_id);
CREATE INDEX parked_line_parked_id ON parked_line (parked_id);
CREATE INDEX grid_tile_store_id_location_id ON grid_tile (store_id, location_id);

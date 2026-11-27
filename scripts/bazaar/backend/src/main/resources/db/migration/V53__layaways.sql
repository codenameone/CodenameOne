-- Wave B: layaways, held on a deposit and paid off in parts.
CREATE TABLE layaway (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    register_id VARCHAR(32) NOT NULL,
    customer_id VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    total_cents BIGINT NOT NULL,
    paid_cents BIGINT NOT NULL,
    cancel_fee_bp BIGINT NOT NULL,
    refunded_cents BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE TABLE layaway_payment (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    layaway_id VARCHAR(32) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    amount_cents BIGINT NOT NULL,
    reference VARCHAR(64) NOT NULL,
    username VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX layaway_order_id ON layaway (order_id);
CREATE INDEX layaway_store_id_status ON layaway (store_id, status);
CREATE INDEX layaway_payment_layaway_id ON layaway_payment (layaway_id);

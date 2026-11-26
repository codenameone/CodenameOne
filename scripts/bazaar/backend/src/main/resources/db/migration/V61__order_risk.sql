-- Wave B: fraud risk per order, and the failed checkout attempts it counts.
CREATE TABLE order_risk (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    score INTEGER NOT NULL,
    level VARCHAR(8) NOT NULL,
    reasons VARCHAR(1000) NOT NULL,
    status VARCHAR(16) NOT NULL,
    reviewed_by VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX order_risk_order_key ON order_risk (order_id);
CREATE INDEX order_risk_store_id ON order_risk (store_id);
CREATE TABLE checkout_failure (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    username VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX checkout_failure_store_user ON checkout_failure (store_id, username);

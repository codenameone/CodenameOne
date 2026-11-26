-- Merchant fraud filter rules: a condition (kind + threshold) and what a
-- match does to the order, HOLD for review or CANCEL outright.
CREATE TABLE fraud_rule (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    kind VARCHAR(24) NOT NULL,
    threshold BIGINT NOT NULL,
    action VARCHAR(8) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX fraud_rule_store_id ON fraud_rule (store_id);

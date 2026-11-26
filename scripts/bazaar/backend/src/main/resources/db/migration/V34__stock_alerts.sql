-- Wave A: stock alerts.

-- Someone waiting to hear that a variant is back in stock; sent once.
CREATE TABLE stock_alert (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    email VARCHAR(190) NOT NULL,
    username VARCHAR(190) NOT NULL,
    channel VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL,
    sent_at BIGINT NOT NULL
);
CREATE INDEX stock_alert_variant_id_status ON stock_alert (variant_id, status);
CREATE INDEX stock_alert_status ON stock_alert (status);
CREATE INDEX stock_alert_store_id ON stock_alert (store_id);

-- A variant sold before it is in stock: when it ships and whether the card is charged now.
CREATE TABLE preorder (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    enabled SMALLINT NOT NULL,
    ship_date BIGINT NOT NULL,
    charge_mode VARCHAR(16) NOT NULL,
    limit_qty INTEGER NOT NULL,
    incoming_qty INTEGER NOT NULL,
    note VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX preorder_u_variant_id ON preorder (variant_id);


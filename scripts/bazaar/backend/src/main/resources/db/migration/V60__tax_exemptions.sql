-- Wave B: customer tax exemptions, and which orders one exempted.
CREATE TABLE tax_exemption (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    username VARCHAR(200) NOT NULL,
    certificate_id VARCHAR(64) NOT NULL,
    regions VARCHAR(400) NOT NULL,
    expires_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX tax_exemption_store_user ON tax_exemption (store_id, username);
CREATE TABLE order_exemption (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    certificate_id VARCHAR(64) NOT NULL
);
CREATE UNIQUE INDEX order_exemption_order_key ON order_exemption (order_id);
CREATE INDEX order_exemption_store_id ON order_exemption (store_id);

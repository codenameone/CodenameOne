-- A checkout link for a draft order or invoice. The URL carries the row's
-- id, its expiry and an HMAC of both; the row lets a link be replaced.
CREATE TABLE pay_link (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    expires_at BIGINT NOT NULL,
    status VARCHAR(12) NOT NULL,
    created_by VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX pay_link_order ON pay_link (order_id);
CREATE INDEX pay_link_store ON pay_link (store_id);

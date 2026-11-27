-- Wave A: sales.

-- A scheduled price change over a set of variants, or a flash sale with a stock cap.
CREATE TABLE sale_event (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    title VARCHAR(120) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    mode VARCHAR(16) NOT NULL,
    amount BIGINT NOT NULL,
    starts_at BIGINT NOT NULL,
    ends_at BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    stock_cap INTEGER NOT NULL,
    sold INTEGER NOT NULL,
    colour VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX sale_event_store_id ON sale_event (store_id);
CREATE INDEX sale_event_status_starts_at ON sale_event (status, starts_at);
CREATE INDEX sale_event_status_ends_at ON sale_event (status, ends_at);

-- A variant in a sale, with the prices it had before the sale changed them.
CREATE TABLE sale_item (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    sale_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    sale_cents BIGINT NOT NULL,
    original_cents BIGINT NOT NULL,
    original_compare_cents BIGINT NOT NULL,
    state VARCHAR(16) NOT NULL
);
CREATE INDEX sale_item_sale_id ON sale_item (sale_id);
CREATE INDEX sale_item_variant_id ON sale_item (variant_id);

-- Units of a flash sale an order took, given back once if the order is cancelled.
CREATE TABLE sale_claim (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    sale_id VARCHAR(32) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    qty INTEGER NOT NULL,
    released SMALLINT NOT NULL
);
CREATE INDEX sale_claim_order_id ON sale_claim (order_id);
CREATE UNIQUE INDEX sale_claim_u_sale_id_order_id ON sale_claim (sale_id, order_id);


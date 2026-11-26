-- The fields a store asks for at checkout, in order, and the answers each
-- order was placed with. An answer keeps the field's label as it read then,
-- so renaming or deleting a field never changes an order already placed.
CREATE TABLE checkout_field (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    sort_order INTEGER NOT NULL,
    label VARCHAR(120) NOT NULL,
    kind VARCHAR(12) NOT NULL,
    choices VARCHAR(1000) NOT NULL,
    required SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX checkout_field_store ON checkout_field (store_id);
CREATE TABLE order_field (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    order_id VARCHAR(32) NOT NULL,
    store_id VARCHAR(32) NOT NULL,
    sort_order INTEGER NOT NULL,
    label VARCHAR(120) NOT NULL,
    answer VARCHAR(500) NOT NULL
);
CREATE INDEX order_field_order ON order_field (order_id);

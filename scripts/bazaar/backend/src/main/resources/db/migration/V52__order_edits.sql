-- Wave B: editing an order after it was placed.
CREATE TABLE order_edit (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    before_cents BIGINT NOT NULL,
    after_cents BIGINT NOT NULL,
    delta_cents BIGINT NOT NULL,
    settlement VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX order_edit_order_id ON order_edit (order_id);
CREATE INDEX order_edit_store_id ON order_edit (store_id);

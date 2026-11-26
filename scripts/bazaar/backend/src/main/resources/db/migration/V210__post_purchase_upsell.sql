-- Post-purchase upsell: the one product a store offers on the order-placed
-- page, and each add-on order taken from it, keyed by the order it follows.
-- The add-on is an order of its own; the paid original is never changed.
CREATE TABLE store_upsell (
    store_id VARCHAR(32) NOT NULL PRIMARY KEY,
    product_id VARCHAR(32) NOT NULL
);
CREATE TABLE upsell_order (
    original_id VARCHAR(32) NOT NULL PRIMARY KEY,
    order_id VARCHAR(32) NOT NULL,
    store_id VARCHAR(32) NOT NULL
);

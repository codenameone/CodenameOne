-- Try before you buy: products a store lets shoppers try for a few days
-- (at most 6, inside a card authorization's life), and the orders placed
-- with one, whose held payment is captured when the trial ends unless the
-- goods were returned first.
CREATE TABLE product_trial (
    product_id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    days INTEGER NOT NULL
);
CREATE INDEX product_trial_store_id ON product_trial (store_id);
CREATE TABLE trial_order (
    order_id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    status VARCHAR(12) NOT NULL,
    ends_at BIGINT NOT NULL
);
CREATE INDEX trial_order_status ON trial_order (status, ends_at);

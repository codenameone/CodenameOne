-- Store credit as grants, each with its own expiry (0 for never), spent
-- from the one that lapses first. customer.store_credit_cents stays the sum
-- of what is left.
CREATE TABLE store_credit_grant (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    customer_id VARCHAR(32) NOT NULL,
    amount_cents BIGINT NOT NULL,
    remaining_cents BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    reason VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX store_credit_grant_customer ON store_credit_grant (customer_id);
CREATE INDEX store_credit_grant_expiry ON store_credit_grant (expires_at);
-- What an order (or a sale) took from each grant, so a cancellation or a
-- refund puts it back where it came from.
CREATE TABLE store_credit_use (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    grant_id VARCHAR(32) NOT NULL,
    customer_id VARCHAR(32) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    amount_cents BIGINT NOT NULL,
    returned_cents BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX store_credit_use_order ON store_credit_use (order_id);
CREATE INDEX store_credit_use_customer ON store_credit_use (customer_id);
-- Balances from before grants: one grant that never lapses.
INSERT INTO store_credit_grant (id, store_id, customer_id, amount_cents, remaining_cents,
        expires_at, reason, created_at)
    SELECT id, store_id, id, store_credit_cents, store_credit_cents, 0, 'Opening balance',
        created_at FROM customer WHERE store_credit_cents > 0;
-- The shopper chose to pay with their store credit.
ALTER TABLE cart ADD COLUMN use_store_credit SMALLINT NOT NULL DEFAULT 0;
-- Codes generated in bulk from a discount carry its id; each is its own
-- single-use discount.
ALTER TABLE discount ADD COLUMN batch_id VARCHAR(32) NOT NULL DEFAULT '';
CREATE INDEX discount_batch ON discount (batch_id);

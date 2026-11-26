-- An express (wallet) checkout under way: the processor's payment intent for
-- a storefront cart, re-quoted as the wallet sheet changes address or
-- delivery, and the order it became once confirmed. The intent id is the
-- order's idempotency key, so a repeated confirm answers the same order.
CREATE TABLE express_checkout (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    cart_id VARCHAR(32) NOT NULL,
    payer VARCHAR(200) NOT NULL,
    provider_ref VARCHAR(64) NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    rate_id VARCHAR(100) NOT NULL,
    status VARCHAR(12) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX express_checkout_store ON express_checkout (store_id);
CREATE INDEX express_checkout_ref ON express_checkout (provider_ref);

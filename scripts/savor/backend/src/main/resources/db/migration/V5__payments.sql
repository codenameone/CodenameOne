CREATE TABLE payment (
    delivery_id VARCHAR(32) NOT NULL PRIMARY KEY,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL,
    provider_ref VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    -- A delivery's payment grows a tip, which is a second charge, and a refund.
    tip_ref VARCHAR(64) NOT NULL,
    refund_ref VARCHAR(64) NOT NULL,
    -- The tip is refunded on its own, and a refund the processor has not
    -- finished is asked about by both references.
    tip_refund_ref VARCHAR(64) NOT NULL,
    refund_reason VARCHAR(255) NOT NULL,
    refunded_at BIGINT NOT NULL
);

-- The account a customer has with the payment processor, made the first time
-- they add a card.
CREATE TABLE payment_customer (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    reference VARCHAR(64) NOT NULL
);

-- A saved card: what the processor calls it, and the few things about it that
-- are safe to show. No card number is ever stored here, or anywhere.
CREATE TABLE payment_method (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    brand VARCHAR(24) NOT NULL,
    last4 VARCHAR(4) NOT NULL,
    exp_month INTEGER NOT NULL,
    exp_year INTEGER NOT NULL,
    token VARCHAR(64) NOT NULL,
    preferred SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX payment_method_user ON payment_method (username, created_at);

-- An attempt to add a card, between its start and its completion.
CREATE TABLE payment_setup (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    reference VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL,
    completed SMALLINT NOT NULL
);

-- Where a courier's payouts go, and the payouts made.
CREATE TABLE payout_account (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    holder VARCHAR(120) NOT NULL,
    bank_name VARCHAR(80) NOT NULL,
    last4 VARCHAR(4) NOT NULL
);

CREATE TABLE payout (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    courier VARCHAR(190) NOT NULL,
    amount_cents BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL,
    destination VARCHAR(120) NOT NULL
);
CREATE INDEX payout_courier ON payout (courier, created_at);

-- What deliveries cost, once an admin has changed it: a single row, id 1. Until
-- there is one the prices are the fare settings of the server.
CREATE TABLE pricing (
    id INTEGER NOT NULL PRIMARY KEY,
    -- The delivery fee: a base, and an amount per kilometre from the
    -- restaurant to the door, times `surge` while couriers are scarce.
    base_cents BIGINT NOT NULL,
    per_km_cents BIGINT NOT NULL,
    surge DOUBLE PRECISION NOT NULL,
    -- An order whose food comes to less than `small_order_cents` pays the
    -- difference, up to `small_order_fee_cents`.
    small_order_cents BIGINT NOT NULL,
    small_order_fee_cents BIGINT NOT NULL,
    -- The customer's service fee and the tax, as percentages of the food.
    fee_percent DOUBLE PRECISION NOT NULL,
    tax_percent DOUBLE PRECISION NOT NULL,
    -- What the service keeps of the food; the restaurant is paid the rest.
    commission_percent DOUBLE PRECISION NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(190) NOT NULL
);

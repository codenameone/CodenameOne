CREATE TABLE wl_payment (
    ride_id VARCHAR(32) NOT NULL PRIMARY KEY,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL,
    provider_ref VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    -- A ride's payment grows a tip, which is a second charge, and a refund.
    tip_ref VARCHAR(64) NOT NULL,
    refund_ref VARCHAR(64) NOT NULL,
    refund_reason VARCHAR(255) NOT NULL,
    refunded_at BIGINT NOT NULL
);

-- The account a rider has with the payment processor, made the first time
-- they add a card.
CREATE TABLE wl_payment_customer (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    reference VARCHAR(64) NOT NULL
);

-- A saved card: what the processor calls it, and the few things about it that
-- are safe to show. No card number is ever stored here, or anywhere.
CREATE TABLE wl_payment_method (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    brand VARCHAR(24) NOT NULL,
    last4 VARCHAR(4) NOT NULL,
    exp_month INTEGER NOT NULL,
    exp_year INTEGER NOT NULL,
    token VARCHAR(64) NOT NULL,
    is_default SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX wl_payment_method_user ON wl_payment_method (username, created_at);

-- An attempt to add a card, between its start and its completion.
CREATE TABLE wl_payment_setup (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    reference VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL,
    completed SMALLINT NOT NULL
);

-- Where a driver's payouts go, and the payouts made.
CREATE TABLE wl_payout_account (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    holder VARCHAR(120) NOT NULL,
    bank_name VARCHAR(80) NOT NULL,
    last4 VARCHAR(4) NOT NULL
);

CREATE TABLE wl_payout (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    driver VARCHAR(190) NOT NULL,
    amount_cents BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL,
    destination VARCHAR(120) NOT NULL
);
CREATE INDEX wl_payout_driver ON wl_payout (driver, created_at);

-- What rides cost, once an admin has changed it: a single row, id 1. Until
-- there is one the prices are the fare settings of the server.
CREATE TABLE wl_pricing (
    id INTEGER NOT NULL PRIMARY KEY,
    base_cents BIGINT NOT NULL,
    per_km_cents BIGINT NOT NULL,
    per_minute_cents BIGINT NOT NULL,
    minimum_cents BIGINT NOT NULL,
    fee_percent DOUBLE PRECISION NOT NULL,
    commission_percent DOUBLE PRECISION NOT NULL,
    surge DOUBLE PRECISION NOT NULL,
    comfort DOUBLE PRECISION NOT NULL,
    xl DOUBLE PRECISION NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(190) NOT NULL
);

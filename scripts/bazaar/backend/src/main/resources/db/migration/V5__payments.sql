CREATE TABLE payment (
    -- The order this payment is for; one payment per order.
    order_id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    method_id VARCHAR(32) NOT NULL,
    method_label VARCHAR(64) NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    -- AUTHORIZED, PAID, VOIDED, FAILED, PARTIALLY_REFUNDED, REFUNDED,
    -- REFUND_PENDING or REFUND_FAILED; CAPTURING, VOIDING and REFUNDING while
    -- a call to the processor is under way.
    status VARCHAR(24) NOT NULL,
    provider_ref VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    paid_at BIGINT NOT NULL,
    -- A captured payment can be refunded, in part or in whole.
    refund_ref VARCHAR(64) NOT NULL,
    refunded_cents BIGINT NOT NULL,
    refund_reason VARCHAR(255) NOT NULL,
    refunded_at BIGINT NOT NULL
);
CREATE INDEX payment_user ON payment (username, created_at);

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

-- Where a merchant's payouts go, and the payouts made.
CREATE TABLE payout_account (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    holder VARCHAR(120) NOT NULL,
    bank_name VARCHAR(80) NOT NULL,
    last4 VARCHAR(4) NOT NULL
);

CREATE TABLE payout (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    payee VARCHAR(190) NOT NULL,
    amount_cents BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL,
    destination VARCHAR(120) NOT NULL
);
CREATE INDEX payout_payee ON payout (payee, created_at);

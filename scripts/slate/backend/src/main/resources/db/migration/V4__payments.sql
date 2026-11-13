-- Saved cards. Money, wherever it appears, is in cents, in a BIGINT.

-- The account a payer has with the payment processor, made the first time
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

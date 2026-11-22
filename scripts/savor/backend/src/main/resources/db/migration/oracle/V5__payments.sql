-- The Oracle version of ../V5__payments.sql, which every other engine shares.
-- Oracle 19c has no BIGINT, INTEGER is NUMBER(38) and DOUBLE PRECISION
-- a decimal float, so the columns are the types the ORM declares there.
-- Text columns are nullable even when the entity requires them: Oracle
-- stores '' as NULL, so a NOT NULL column could never hold an empty
-- string; the ORM enforces 'required' itself and reads NULL back as ''.
-- A DEFAULT comes before NOT NULL, the only order Oracle accepts, and a
-- column named by an Oracle reserved word is quoted.

CREATE TABLE payment (
    delivery_id VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    amount_cents NUMBER(19) NOT NULL,
    currency VARCHAR2(3 CHAR),
    status VARCHAR2(16 CHAR),
    provider_ref VARCHAR2(64 CHAR),
    created_at NUMBER(19) NOT NULL,
    -- A delivery's payment grows a tip, which is a second charge, and a refund.
    tip_ref VARCHAR2(64 CHAR),
    refund_ref VARCHAR2(64 CHAR),
    -- The tip is refunded on its own, and a refund the processor has not
    -- finished is asked about by both references.
    tip_refund_ref VARCHAR2(64 CHAR),
    refund_reason VARCHAR2(255 CHAR),
    refunded_at NUMBER(19) NOT NULL
);

-- The account a customer has with the payment processor, made the first time
-- they add a card.
CREATE TABLE payment_customer (
    username VARCHAR2(190 CHAR) NOT NULL PRIMARY KEY,
    reference VARCHAR2(64 CHAR)
);

-- A saved card: what the processor calls it, and the few things about it that
-- are safe to show. No card number is ever stored here, or anywhere.
CREATE TABLE payment_method (
    id VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    username VARCHAR2(190 CHAR),
    brand VARCHAR2(24 CHAR),
    last4 VARCHAR2(4 CHAR),
    exp_month NUMBER(10) NOT NULL,
    exp_year NUMBER(10) NOT NULL,
    token VARCHAR2(64 CHAR),
    preferred NUMBER(5) NOT NULL,
    created_at NUMBER(19) NOT NULL
);
CREATE INDEX payment_method_user ON payment_method (username, created_at);

-- An attempt to add a card, between its start and its completion.
CREATE TABLE payment_setup (
    id VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    username VARCHAR2(190 CHAR),
    reference VARCHAR2(255 CHAR),
    created_at NUMBER(19) NOT NULL,
    completed NUMBER(5) NOT NULL
);

-- Where a courier's payouts go, and the payouts made.
CREATE TABLE payout_account (
    username VARCHAR2(190 CHAR) NOT NULL PRIMARY KEY,
    holder VARCHAR2(120 CHAR),
    bank_name VARCHAR2(80 CHAR),
    last4 VARCHAR2(4 CHAR)
);

CREATE TABLE payout (
    id VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    courier VARCHAR2(190 CHAR),
    amount_cents NUMBER(19) NOT NULL,
    status VARCHAR2(16 CHAR),
    created_at NUMBER(19) NOT NULL,
    destination VARCHAR2(120 CHAR)
);
CREATE INDEX payout_courier ON payout (courier, created_at);

-- What deliveries cost, once an admin has changed it: a single row, id 1. Until
-- there is one the prices are the fare settings of the server.
CREATE TABLE pricing (
    id NUMBER(10) NOT NULL PRIMARY KEY,
    -- The delivery fee: a base, and an amount per kilometre from the
    -- restaurant to the door, times `surge` while couriers are scarce.
    base_cents NUMBER(19) NOT NULL,
    per_km_cents NUMBER(19) NOT NULL,
    surge BINARY_DOUBLE NOT NULL,
    -- An order whose food comes to less than `small_order_cents` pays the
    -- difference, up to `small_order_fee_cents`.
    small_order_cents NUMBER(19) NOT NULL,
    small_order_fee_cents NUMBER(19) NOT NULL,
    -- The customer's service fee and the tax, as percentages of the food.
    fee_percent BINARY_DOUBLE NOT NULL,
    tax_percent BINARY_DOUBLE NOT NULL,
    -- What the service keeps of the food; the restaurant is paid the rest.
    commission_percent BINARY_DOUBLE NOT NULL,
    updated_at NUMBER(19) NOT NULL,
    updated_by VARCHAR2(190 CHAR)
);

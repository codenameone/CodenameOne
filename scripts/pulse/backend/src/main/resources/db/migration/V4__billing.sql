-- Memberships and what they cost. Money is in cents, in a BIGINT.

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

-- What can be bought. MONTHLY and ANNUAL renew; a CLASS_PACK is a number of
-- classes that lasts `period_months`; a DROP_IN is one class.
CREATE TABLE membership_plan (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(500) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    price_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    period_months INTEGER NOT NULL,
    class_credits INTEGER NOT NULL,
    min_age INTEGER NOT NULL,
    max_age INTEGER NOT NULL,
    off_peak_only SMALLINT NOT NULL,
    active SMALLINT NOT NULL
);

-- A member's plan. `current_period_end` is when the next invoice is raised;
-- a freeze moves it on by the frozen days.
CREATE TABLE membership (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    member_id VARCHAR(32) NOT NULL,
    account VARCHAR(190) NOT NULL,
    plan_id VARCHAR(32) NOT NULL,
    state VARCHAR(16) NOT NULL,
    started_at BIGINT NOT NULL,
    current_period_end BIGINT NOT NULL,
    credits_left INTEGER NOT NULL,
    frozen_from BIGINT NOT NULL,
    frozen_until BIGINT NOT NULL,
    cancel_at_period_end SMALLINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX membership_member ON membership (member_id, state);
CREATE INDEX membership_due ON membership (state, current_period_end);

-- A bill. The key it is charged under is its id, so a charge retried after a
-- timeout is the same charge at the processor. A failed charge is tried again
-- at `next_attempt_at`, on the dunning ladder; `attempts` counts the tries.
CREATE TABLE invoice (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    account VARCHAR(190) NOT NULL,
    membership_id VARCHAR(32) NOT NULL,
    member_id VARCHAR(32) NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount_cents BIGINT NOT NULL,
    credit_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL,
    period_start BIGINT NOT NULL,
    period_end BIGINT NOT NULL,
    attempts INTEGER NOT NULL,
    next_attempt_at BIGINT NOT NULL,
    charge_ref VARCHAR(64) NOT NULL,
    method_id VARCHAR(32) NOT NULL,
    failure VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL,
    paid_at BIGINT NOT NULL
);
CREATE INDEX invoice_account ON invoice (account, created_at);
CREATE INDEX invoice_due ON invoice (status, next_attempt_at);

-- Money owed to an account: a referral reward, a goodwill credit. Taken off
-- the next invoice.
CREATE TABLE credit (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    account VARCHAR(190) NOT NULL,
    amount_cents BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL,
    invoice_id VARCHAR(32) NOT NULL
);
CREATE INDEX credit_account ON credit (account, invoice_id);

-- Wave A: loyalty.

-- A store's points programme: how points are earned, what they are worth and when they lapse.
CREATE TABLE loyalty_program (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    active SMALLINT NOT NULL,
    points_per_unit INTEGER NOT NULL,
    unit_cents BIGINT NOT NULL,
    rounding VARCHAR(16) NOT NULL,
    points_per_reward INTEGER NOT NULL,
    reward_cents BIGINT NOT NULL,
    min_redeem_points INTEGER NOT NULL,
    expiry_days INTEGER NOT NULL,
    earn_on_pos SMALLINT NOT NULL,
    tier_basis VARCHAR(16) NOT NULL,
    referrer_points INTEGER NOT NULL,
    referee_points INTEGER NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX loyalty_program_store_id ON loyalty_program (store_id);

-- A level of a loyalty programme, reached by spend or points in the last twelve months.
CREATE TABLE loyalty_tier (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    name VARCHAR(80) NOT NULL,
    threshold BIGINT NOT NULL,
    member_bps INTEGER NOT NULL,
    free_shipping SMALLINT NOT NULL,
    early_access_hours INTEGER NOT NULL,
    colour VARCHAR(16) NOT NULL,
    position INTEGER NOT NULL,
    discount_id VARCHAR(32) NOT NULL,
    shipping_discount_id VARCHAR(32) NOT NULL
);
CREATE INDEX loyalty_tier_store_id ON loyalty_tier (store_id);

-- A customer's standing in a store's programme: balance, tier, card and referral code.
CREATE TABLE loyalty_member (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    customer_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    email VARCHAR(190) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    balance BIGINT NOT NULL,
    lifetime BIGINT NOT NULL,
    tier_id VARCHAR(32) NOT NULL,
    card_token VARCHAR(40) NOT NULL,
    referral_code VARCHAR(16) NOT NULL,
    referred_by VARCHAR(32) NOT NULL,
    referral_rewarded SMALLINT NOT NULL,
    last_activity_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    version BIGINT NOT NULL
);
CREATE INDEX loyalty_member_store_id_customer_id ON loyalty_member (store_id, customer_id);
CREATE INDEX loyalty_member_store_id_username ON loyalty_member (store_id, username);
CREATE UNIQUE INDEX loyalty_member_u_card_token ON loyalty_member (card_token);
CREATE UNIQUE INDEX loyalty_member_u_store_id_referral_code ON loyalty_member (store_id, referral_code);

-- One movement of a member's points; `ref` is unique, which makes every earn and reversal happen once.
CREATE TABLE loyalty_ledger (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    member_id VARCHAR(32) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    points BIGINT NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    ref VARCHAR(80) NOT NULL,
    note VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX loyalty_ledger_member_id ON loyalty_ledger (member_id);
CREATE INDEX loyalty_ledger_store_id_order_id ON loyalty_ledger (store_id, order_id);
CREATE UNIQUE INDEX loyalty_ledger_u_store_id_ref ON loyalty_ledger (store_id, ref);

-- Points turned into a single-use discount code.
CREATE TABLE loyalty_reward (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    member_id VARCHAR(32) NOT NULL,
    points BIGINT NOT NULL,
    cents BIGINT NOT NULL,
    discount_id VARCHAR(32) NOT NULL,
    code VARCHAR(40) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL
);
CREATE INDEX loyalty_reward_member_id ON loyalty_reward (member_id);
CREATE INDEX loyalty_reward_discount_id ON loyalty_reward (discount_id);


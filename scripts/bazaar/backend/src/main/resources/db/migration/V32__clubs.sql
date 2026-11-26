-- Wave A: clubs.

-- A consumer club: optional paid membership, member prices, member-only products and early access.
CREATE TABLE club (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    fee_cents BIGINT NOT NULL,
    period_days INTEGER NOT NULL,
    member_bps INTEGER NOT NULL,
    early_access_hours INTEGER NOT NULL,
    colour VARCHAR(16) NOT NULL,
    active SMALLINT NOT NULL,
    discount_id VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX club_store_id ON club (store_id);

-- A customer's membership of a club.
CREATE TABLE club_member (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    club_id VARCHAR(32) NOT NULL,
    customer_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    status VARCHAR(16) NOT NULL,
    payment_method_id VARCHAR(64) NOT NULL,
    joined_at BIGINT NOT NULL,
    renews_at BIGINT NOT NULL,
    last_charge_id VARCHAR(32) NOT NULL,
    failures INTEGER NOT NULL
);
CREATE INDEX club_member_club_id_username ON club_member (club_id, username);
CREATE INDEX club_member_status_renews_at ON club_member (status, renews_at);
CREATE INDEX club_member_store_id_username ON club_member (store_id, username);

-- A product or collection a club holds back: members only, or early access before `publicFrom`.
CREATE TABLE club_item (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    club_id VARCHAR(32) NOT NULL,
    product_id VARCHAR(32) NOT NULL,
    collection_id VARCHAR(32) NOT NULL,
    members_only SMALLINT NOT NULL,
    members_from BIGINT NOT NULL,
    public_from BIGINT NOT NULL
);
CREATE INDEX club_item_club_id ON club_item (club_id);
CREATE INDEX club_item_product_id ON club_item (product_id);
CREATE INDEX club_item_store_id ON club_item (store_id);


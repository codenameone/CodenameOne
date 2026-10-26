-- Money movement: who may be paid, the cards, the devices that sign for a
-- customer, the nonces those signatures have used, and one row per movement
-- the limits are counted from.

CREATE TABLE payee (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    owner VARCHAR(190) NOT NULL,
    username VARCHAR(190) NOT NULL,
    nickname VARCHAR(80) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX payee_owner_username ON payee (owner, username);

-- Only the last four digits. The full number is the card processor's; a
-- database that never held it cannot leak it.
CREATE TABLE card (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    owner VARCHAR(190) NOT NULL,
    account_id VARCHAR(32) NOT NULL,
    last4 VARCHAR(4) NOT NULL,
    frozen SMALLINT NOT NULL,
    daily_limit BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX card_account ON card (account_id);

CREATE TABLE device (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    owner VARCHAR(190) NOT NULL,
    public_key VARCHAR(400) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    rooted SMALLINT NOT NULL,
    attested SMALLINT NOT NULL,
    revoked SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX device_owner ON device (owner);

-- A nonce is kept until its signature could no longer pass the clock check,
-- then purged. The key is sha256(device|nonce), so the unique index is what
-- refuses a replay, atomically, however many copies arrive at once.
CREATE TABLE used_nonce (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    expires_at BIGINT NOT NULL
);
CREATE INDEX used_nonce_expiry ON used_nonce (expires_at);

-- One row per movement, keyed by its journal entry: what the limits add up
-- and what a retried request's receipt is rebuilt from. `via` is the payee or
-- the card the money went through.
CREATE TABLE transfer (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    owner VARCHAR(190) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    from_account VARCHAR(32) NOT NULL,
    to_account VARCHAR(32) NOT NULL,
    via VARCHAR(32) NOT NULL,
    asset VARCHAR(12) NOT NULL,
    amount BIGINT NOT NULL,
    outgoing SMALLINT NOT NULL,
    memo VARCHAR(140) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX transfer_owner ON transfer (owner, created_at);
CREATE INDEX transfer_via ON transfer (via, created_at);

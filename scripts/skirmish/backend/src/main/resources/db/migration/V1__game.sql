-- The tables every database shares. Portable SQL only: no generated keys, no
-- native boolean (a yes/no is a SMALLINT 0/1), times are epoch milliseconds.
-- The replay table differs per database and lives in V2 beside this directory.

CREATE TABLE profile (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    display_name VARCHAR(40) NOT NULL,
    trophies INTEGER NOT NULL,
    wins INTEGER NOT NULL,
    losses INTEGER NOT NULL,
    draws INTEGER NOT NULL,
    coins INTEGER NOT NULL,
    gems INTEGER NOT NULL,
    deck VARCHAR(255) NOT NULL,
    owned VARCHAR(1000) NOT NULL,
    arena VARCHAR(40) NOT NULL,
    flagged SMALLINT NOT NULL,
    muted SMALLINT NOT NULL,
    suspended_until BIGINT NOT NULL,
    banned SMALLINT NOT NULL,
    created_at BIGINT NOT NULL,
    last_seen BIGINT NOT NULL,
    ad_day BIGINT NOT NULL,
    ad_count INTEGER NOT NULL,
    left_streak INTEGER NOT NULL
);
CREATE INDEX profile_trophies ON profile (trophies);

CREATE TABLE flag (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    match_id VARCHAR(32) NOT NULL,
    reporter VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL,
    pending SMALLINT NOT NULL,
    resolved_by VARCHAR(190) NOT NULL
);
CREATE INDEX flag_username ON flag (username, created_at);
CREATE INDEX flag_pending ON flag (pending, created_at);

CREATE TABLE admin_action (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    admin VARCHAR(190) NOT NULL,
    username VARCHAR(190) NOT NULL,
    action VARCHAR(24) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX admin_action_username ON admin_action (username, created_at);

CREATE TABLE store_purchase (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    store VARCHAR(16) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    gems INTEGER NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX store_purchase_username ON store_purchase (username);

CREATE TABLE game_ticket (
    ticket_hash VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    expires_at BIGINT NOT NULL
);

CREATE TABLE push_device (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    provider VARCHAR(16) NOT NULL,
    token VARCHAR(1024) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX push_device_username ON push_device (username);

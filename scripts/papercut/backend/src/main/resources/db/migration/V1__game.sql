-- The tables every database shares. Portable SQL only: no generated keys, no
-- native boolean (a yes/no is a SMALLINT 0/1), times are epoch milliseconds.
-- Pushes, purchases and the audit log are at the end.

CREATE TABLE profile (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    display_name VARCHAR(40) NOT NULL,
    ink_coins INTEGER NOT NULL,
    erasers INTEGER NOT NULL,
    best_classic BIGINT NOT NULL,
    best_fold BIGINT NOT NULL,
    best_daily BIGINT NOT NULL,
    runs INTEGER NOT NULL,
    quill VARCHAR(40) NOT NULL,
    owned VARCHAR(1000) NOT NULL,
    flagged SMALLINT NOT NULL,
    suspended_until BIGINT NOT NULL,
    banned SMALLINT NOT NULL,
    created_at BIGINT NOT NULL,
    last_seen BIGINT NOT NULL
);
CREATE INDEX profile_best_classic ON profile (best_classic);

CREATE TABLE score_run (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    mode VARCHAR(16) NOT NULL,
    score BIGINT NOT NULL,
    kills INTEGER NOT NULL,
    gates INTEGER NOT NULL,
    seconds INTEGER NOT NULL,
    peak_multiplier INTEGER NOT NULL,
    day BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    struck SMALLINT NOT NULL
);
CREATE INDEX score_run_board ON score_run (mode, created_at);
CREATE INDEX score_run_username ON score_run (username, created_at);

CREATE TABLE run_ticket (
    token VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    mode VARCHAR(16) NOT NULL,
    seed BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    used SMALLINT NOT NULL
);
CREATE INDEX run_ticket_expires ON run_ticket (expires_at);

CREATE TABLE flag (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    run_id VARCHAR(32) NOT NULL,
    mode VARCHAR(16) NOT NULL,
    score BIGINT NOT NULL,
    kills INTEGER NOT NULL,
    seconds INTEGER NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL,
    pending SMALLINT NOT NULL,
    resolved_by VARCHAR(190) NOT NULL
);
CREATE INDEX flag_pending ON flag (pending, created_at);
CREATE INDEX flag_username ON flag (username);

CREATE TABLE friend (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    friend VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX friend_username ON friend (username);

CREATE TABLE daily_seed (
    day VARCHAR(10) NOT NULL PRIMARY KEY,
    seed BIGINT NOT NULL,
    set_by VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE ad_nonce (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE broadcast (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    title VARCHAR(80) NOT NULL,
    body VARCHAR(255) NOT NULL,
    days INTEGER NOT NULL,
    targets INTEGER NOT NULL,
    created_at BIGINT NOT NULL
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

CREATE TABLE store_purchase (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    store VARCHAR(16) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    coins INTEGER NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX store_purchase_username ON store_purchase (username);

CREATE TABLE admin_action (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    admin VARCHAR(190) NOT NULL,
    username VARCHAR(190) NOT NULL,
    action VARCHAR(24) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX admin_action_created ON admin_action (created_at);

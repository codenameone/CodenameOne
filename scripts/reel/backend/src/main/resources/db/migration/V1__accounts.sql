-- Every script here is written once for SQLite, PostgreSQL and MySQL: no
-- generated keys, no native boolean or timestamp. A yes/no is a SMALLINT 0/1,
-- a time is milliseconds since the epoch in a BIGINT, and a list is a
-- comma-separated VARCHAR.

-- What Reel knows about an account beyond its sign-in, keyed by the same username.
CREATE TABLE profile (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL,
    created_at BIGINT NOT NULL,
    language VARCHAR(8) NOT NULL,
    flagged SMALLINT NOT NULL,
    flag_reason VARCHAR(255) NOT NULL,
    last_seen_at BIGINT NOT NULL
);

CREATE TABLE closed_account (
    username_hash VARCHAR(64) NOT NULL PRIMARY KEY,
    closed_at BIGINT NOT NULL
);

CREATE TABLE moderation_event (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL,
    actor VARCHAR(190) NOT NULL,
    action VARCHAR(16) NOT NULL,
    reason VARCHAR(255) NOT NULL
);
CREATE INDEX moderation_event_username_created_at ON moderation_event (username, created_at);

-- A viewer profile inside an account: its own list, history and rating limit.
CREATE TABLE household (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    name VARCHAR(60) NOT NULL,
    avatar VARCHAR(32) NOT NULL,
    kids SMALLINT NOT NULL,
    max_rating VARCHAR(8) NOT NULL,
    pin_hash VARCHAR(128) NOT NULL,
    language VARCHAR(8) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX household_username ON household (username);

-- A device that signed in to an account, keyed by the id the app made for itself.
CREATE TABLE device (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    name VARCHAR(80) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    last_ip VARCHAR(64) NOT NULL,
    last_country VARCHAR(8) NOT NULL,
    last_seen_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    revoked_at BIGINT NOT NULL,
    grant_id VARCHAR(190) NOT NULL
);
CREATE INDEX device_username ON device (username);

-- One admin-controlled setting: a feature switch, the preset, the branding.
CREATE TABLE setting (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by VARCHAR(190) NOT NULL
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

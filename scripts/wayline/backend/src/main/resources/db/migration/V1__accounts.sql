-- What the app knows about an account beyond its sign-in. The sign-in itself --
-- the password hash, the roles, whether the account is enabled -- lives in the
-- security tables the backend creates (cn1.security.schema.enabled), keyed by
-- the same username.
--
-- Every script here is written once for SQLite, PostgreSQL and MySQL: no
-- generated keys, no native boolean or timestamp. A yes/no is a SMALLINT 0/1 and
-- a time is milliseconds since the epoch in a BIGINT.
CREATE TABLE wl_profile (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    phone_verified SMALLINT NOT NULL,
    vehicle VARCHAR(120) NOT NULL,
    plate VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL,
    -- What an account says about itself beyond its name and phone, and what an
    -- admin has said about it. Added to the profile one column at a time, each with
    -- a default: that is the one form of ALTER TABLE all three engines accept, and
    -- the default is what the accounts opened before this script read as.
    gender VARCHAR(16) NOT NULL,
    lang VARCHAR(8) NOT NULL,
    flagged SMALLINT NOT NULL,
    flag_reason VARCHAR(255) NOT NULL,
    emergency_name VARCHAR(120) NOT NULL,
    emergency_phone VARCHAR(32) NOT NULL,
    home_address VARCHAR(255) NOT NULL,
    work_address VARCHAR(255) NOT NULL
);

-- How one person wants the app and their rides. A row exists once they have
-- saved something; until then the defaults are the server's.
CREATE TABLE wl_preferences (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    theme VARCHAR(8) NOT NULL,
    units VARCHAR(4) NOT NULL,
    notify_rides SMALLINT NOT NULL,
    notify_receipts SMALLINT NOT NULL,
    notify_promotions SMALLINT NOT NULL,
    driver_gender VARCHAR(8) NOT NULL,
    quiet_ride SMALLINT NOT NULL,
    wheelchair SMALLINT NOT NULL,
    pet_friendly SMALLINT NOT NULL,
    default_tip INTEGER NOT NULL,
    share_trips_with VARCHAR(32) NOT NULL
);

-- The verification code an account is waiting on. Only its hash is kept.
CREATE TABLE wl_phone_code (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    code_hash VARCHAR(64) NOT NULL,
    expires_at BIGINT NOT NULL,
    attempts INTEGER NOT NULL,
    sent_at BIGINT NOT NULL
);

-- Tickets for opening the live channel. A row is deleted by the connection that
-- presents it, which is what makes a ticket good for one connection only -- and
-- keeping them here and not in memory is what lets a second server instance
-- redeem a ticket the first one issued.
CREATE TABLE wl_live_ticket (
    ticket_hash VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    expires_at BIGINT NOT NULL
);

-- What the admins did to an account and why: flagged it, blocked it, approved
-- or rejected its driving application. Never changed once written.
CREATE TABLE wl_moderation_event (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL,
    actor VARCHAR(190) NOT NULL,
    action VARCHAR(16) NOT NULL,
    reason VARCHAR(255) NOT NULL
);
CREATE INDEX wl_moderation_event_user ON wl_moderation_event (username, created_at);

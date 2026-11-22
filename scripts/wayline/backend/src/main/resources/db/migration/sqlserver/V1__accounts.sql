-- The SQL Server version of ../V1__accounts.sql, which every other engine shares.
-- Text is NVARCHAR, since VARCHAR holds only the database's code page,
-- under a binary collation: the default one folds case, and a key must
-- keep "A" and "a" apart. A key column cannot change collation once
-- created, so it is declared here rather than converted later.

-- What the app knows about an account beyond its sign-in. The sign-in itself --
-- the password hash, the roles, whether the account is enabled -- lives in the
-- security tables the backend creates (cn1.security.schema.enabled), keyed by
-- the same username.
--
-- Every script here is written once for SQLite, PostgreSQL and MySQL: no
-- generated keys, no native boolean or timestamp. A yes/no is a SMALLINT 0/1 and
-- a time is milliseconds since the epoch in a BIGINT.
CREATE TABLE profile (
    username NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    display_name NVARCHAR(120) COLLATE Latin1_General_100_BIN2 NOT NULL,
    phone NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    phone_verified SMALLINT NOT NULL,
    vehicle NVARCHAR(120) COLLATE Latin1_General_100_BIN2 NOT NULL,
    plate NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    created_at BIGINT NOT NULL,
    -- What an account says about itself beyond its name and phone, and what an
    -- admin has said about it. Added to the profile one column at a time, each with
    -- a default: that is the one form of ALTER TABLE all three engines accept, and
    -- the default is what the accounts opened before this script read as.
    gender NVARCHAR(16) COLLATE Latin1_General_100_BIN2 NOT NULL,
    language NVARCHAR(8) COLLATE Latin1_General_100_BIN2 NOT NULL,
    flagged SMALLINT NOT NULL,
    flag_reason NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL,
    emergency_name NVARCHAR(120) COLLATE Latin1_General_100_BIN2 NOT NULL,
    emergency_phone NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    home_address NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL,
    work_address NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL
);

-- How one person wants the app and their rides. A row exists once they have
-- saved something; until then the defaults are the server's.
CREATE TABLE preference (
    username NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    theme NVARCHAR(8) COLLATE Latin1_General_100_BIN2 NOT NULL,
    units NVARCHAR(4) COLLATE Latin1_General_100_BIN2 NOT NULL,
    notify_rides SMALLINT NOT NULL,
    notify_receipts SMALLINT NOT NULL,
    notify_promotions SMALLINT NOT NULL,
    driver_gender NVARCHAR(8) COLLATE Latin1_General_100_BIN2 NOT NULL,
    quiet_ride SMALLINT NOT NULL,
    wheelchair SMALLINT NOT NULL,
    pet_friendly SMALLINT NOT NULL,
    default_tip INTEGER NOT NULL,
    share_trips_with NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL
);

-- The e-mails of accounts that were closed, as hashes. A closed account's rides
-- stay under its e-mail, so the address is never registered again; the hash is
-- enough to refuse it without keeping a list of who has left.
CREATE TABLE closed_account (
    username_hash NVARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    closed_at BIGINT NOT NULL
);

-- The verification code an account is waiting on. Only its hash is kept, and
-- that of the code before it while it is still good: text messages are not
-- delivered in the order they were sent.
CREATE TABLE phone_code (
    username NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    code_hash NVARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL,
    expires_at BIGINT NOT NULL,
    attempts INTEGER NOT NULL,
    sent_at BIGINT NOT NULL,
    previous_hash NVARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL DEFAULT '',
    previous_expires_at BIGINT NOT NULL DEFAULT 0,
    previous_attempts INTEGER NOT NULL DEFAULT 0
);

-- Tickets for opening the live channel. A row is deleted by the connection that
-- presents it, which is what makes a ticket good for one connection only -- and
-- keeping them here and not in memory is what lets a second server instance
-- redeem a ticket the first one issued.
CREATE TABLE live_ticket (
    ticket_hash NVARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    username NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    expires_at BIGINT NOT NULL
);

-- What the admins did to an account and why: flagged it, blocked it, approved
-- or rejected its driving application. Never changed once written.
CREATE TABLE moderation_event (
    id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    username NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    created_at BIGINT NOT NULL,
    actor NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    action NVARCHAR(16) COLLATE Latin1_General_100_BIN2 NOT NULL,
    reason NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL
);
CREATE INDEX moderation_event_user ON moderation_event (username, created_at);

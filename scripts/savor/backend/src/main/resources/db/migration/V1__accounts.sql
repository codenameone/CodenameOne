-- What the app knows about an account beyond its sign-in. The sign-in itself --
-- the password hash, the roles, whether the account is enabled -- lives in the
-- security tables the backend creates (cn1.security.schema.enabled), keyed by
-- the same username.
--
-- Every script here is written once for SQLite, PostgreSQL and MySQL: no
-- generated keys, no native boolean or timestamp. A yes/no is a SMALLINT 0/1 and
-- a time is milliseconds since the epoch in a BIGINT.
CREATE TABLE profile (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    phone_verified SMALLINT NOT NULL,
    vehicle VARCHAR(120) NOT NULL,
    plate VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL,
    language VARCHAR(8) NOT NULL,
    flagged SMALLINT NOT NULL,
    flag_reason VARCHAR(255) NOT NULL
);

-- How one person wants the app and their deliveries. A row exists once they have
-- saved something; until then the defaults are the server's.
CREATE TABLE preference (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    theme VARCHAR(8) NOT NULL,
    units VARCHAR(4) NOT NULL,
    notify_deliveries SMALLINT NOT NULL,
    notify_receipts SMALLINT NOT NULL,
    notify_promotions SMALLINT NOT NULL,
    default_tip INTEGER NOT NULL
);

-- The e-mails of accounts that were closed, as hashes. A closed account's deliveries
-- stay under its e-mail, so the address is never registered again; the hash is
-- enough to refuse it without keeping a list of who has left.
CREATE TABLE closed_account (
    username_hash VARCHAR(64) NOT NULL PRIMARY KEY,
    closed_at BIGINT NOT NULL
);

-- The verification code an account is waiting on. Only its hash is kept, and
-- that of the code before it while it is still good: text messages are not
-- delivered in the order they were sent.
CREATE TABLE phone_code (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    code_hash VARCHAR(64) NOT NULL,
    expires_at BIGINT NOT NULL,
    attempts INTEGER NOT NULL,
    sent_at BIGINT NOT NULL,
    previous_hash VARCHAR(64) NOT NULL DEFAULT '',
    previous_expires_at BIGINT NOT NULL DEFAULT 0,
    previous_attempts INTEGER NOT NULL DEFAULT 0
);

-- Tickets for opening the live channel. A row is deleted by the connection that
-- presents it, which is what makes a ticket good for one connection only -- and
-- keeping them here and not in memory is what lets a second server instance
-- redeem a ticket the first one issued.
CREATE TABLE live_ticket (
    ticket_hash VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    expires_at BIGINT NOT NULL
);

-- What the admins did to an account and why: flagged it, blocked it, approved
-- or rejected its onboarding application. Never changed once written.
CREATE TABLE moderation_event (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL,
    actor VARCHAR(190) NOT NULL,
    action VARCHAR(16) NOT NULL,
    reason VARCHAR(255) NOT NULL
);
CREATE INDEX moderation_event_user ON moderation_event (username, created_at);

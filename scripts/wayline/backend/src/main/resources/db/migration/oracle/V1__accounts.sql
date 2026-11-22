-- The Oracle version of ../V1__accounts.sql, which every other engine shares.
-- Oracle 19c has no BIGINT, INTEGER is NUMBER(38) and DOUBLE PRECISION
-- a decimal float, so the columns are the types the ORM declares there.
-- Text columns are nullable even when the entity requires them: Oracle
-- stores '' as NULL, so a NOT NULL column could never hold an empty
-- string; the ORM enforces 'required' itself and reads NULL back as ''.
-- A DEFAULT comes before NOT NULL, the only order Oracle accepts, and a
-- column named by an Oracle reserved word is quoted.

-- What the app knows about an account beyond its sign-in. The sign-in itself --
-- the password hash, the roles, whether the account is enabled -- lives in the
-- security tables the backend creates (cn1.security.schema.enabled), keyed by
-- the same username.
--
-- Every script here is written once for SQLite, PostgreSQL and MySQL: no
-- generated keys, no native boolean or timestamp. A yes/no is a SMALLINT 0/1 and
-- a time is milliseconds since the epoch in a BIGINT.
CREATE TABLE profile (
    username VARCHAR2(190 CHAR) NOT NULL PRIMARY KEY,
    display_name VARCHAR2(120 CHAR),
    phone VARCHAR2(32 CHAR),
    phone_verified NUMBER(5) NOT NULL,
    vehicle VARCHAR2(120 CHAR),
    plate VARCHAR2(32 CHAR),
    created_at NUMBER(19) NOT NULL,
    -- What an account says about itself beyond its name and phone, and what an
    -- admin has said about it. Added to the profile one column at a time, each with
    -- a default: that is the one form of ALTER TABLE all three engines accept, and
    -- the default is what the accounts opened before this script read as.
    gender VARCHAR2(16 CHAR),
    language VARCHAR2(8 CHAR),
    flagged NUMBER(5) NOT NULL,
    flag_reason VARCHAR2(255 CHAR),
    emergency_name VARCHAR2(120 CHAR),
    emergency_phone VARCHAR2(32 CHAR),
    home_address VARCHAR2(255 CHAR),
    work_address VARCHAR2(255 CHAR)
);

-- How one person wants the app and their rides. A row exists once they have
-- saved something; until then the defaults are the server's.
CREATE TABLE preference (
    username VARCHAR2(190 CHAR) NOT NULL PRIMARY KEY,
    theme VARCHAR2(8 CHAR),
    units VARCHAR2(4 CHAR),
    notify_rides NUMBER(5) NOT NULL,
    notify_receipts NUMBER(5) NOT NULL,
    notify_promotions NUMBER(5) NOT NULL,
    driver_gender VARCHAR2(8 CHAR),
    quiet_ride NUMBER(5) NOT NULL,
    wheelchair NUMBER(5) NOT NULL,
    pet_friendly NUMBER(5) NOT NULL,
    default_tip NUMBER(10) NOT NULL,
    share_trips_with VARCHAR2(32 CHAR)
);

-- The e-mails of accounts that were closed, as hashes. A closed account's rides
-- stay under its e-mail, so the address is never registered again; the hash is
-- enough to refuse it without keeping a list of who has left.
CREATE TABLE closed_account (
    username_hash VARCHAR2(64 CHAR) NOT NULL PRIMARY KEY,
    closed_at NUMBER(19) NOT NULL
);

-- The verification code an account is waiting on. Only its hash is kept, and
-- that of the code before it while it is still good: text messages are not
-- delivered in the order they were sent.
CREATE TABLE phone_code (
    username VARCHAR2(190 CHAR) NOT NULL PRIMARY KEY,
    code_hash VARCHAR2(64 CHAR),
    expires_at NUMBER(19) NOT NULL,
    attempts NUMBER(10) NOT NULL,
    sent_at NUMBER(19) NOT NULL,
    previous_hash VARCHAR2(64 CHAR) DEFAULT '',
    previous_expires_at NUMBER(19) DEFAULT 0 NOT NULL,
    previous_attempts NUMBER(10) DEFAULT 0 NOT NULL
);

-- Tickets for opening the live channel. A row is deleted by the connection that
-- presents it, which is what makes a ticket good for one connection only -- and
-- keeping them here and not in memory is what lets a second server instance
-- redeem a ticket the first one issued.
CREATE TABLE live_ticket (
    ticket_hash VARCHAR2(64 CHAR) NOT NULL PRIMARY KEY,
    username VARCHAR2(190 CHAR),
    expires_at NUMBER(19) NOT NULL
);

-- What the admins did to an account and why: flagged it, blocked it, approved
-- or rejected its driving application. Never changed once written.
CREATE TABLE moderation_event (
    id VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    username VARCHAR2(190 CHAR),
    created_at NUMBER(19) NOT NULL,
    actor VARCHAR2(190 CHAR),
    action VARCHAR2(16 CHAR),
    reason VARCHAR2(255 CHAR)
);
CREATE INDEX moderation_event_user ON moderation_event (username, created_at);

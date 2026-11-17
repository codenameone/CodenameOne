-- Accounts: who someone is and what they chose. The sign-in itself (password
-- hash, roles) is the backend's own user store and makes its own tables.

CREATE TABLE profile (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    language VARCHAR(8) NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE preference (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    theme VARCHAR(8) NOT NULL,
    notify_assignments SMALLINT NOT NULL,
    notify_schedule_changes SMALLINT NOT NULL,
    calendar_sync SMALLINT NOT NULL,
    ai_assist SMALLINT NOT NULL
);

CREATE TABLE closed_account (
    username_hash VARCHAR(64) NOT NULL PRIMARY KEY,
    closed_at BIGINT NOT NULL
);

CREATE TABLE live_ticket (
    ticket_hash VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    expires_at BIGINT NOT NULL
);

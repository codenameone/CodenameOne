-- The SQL Server version of ../V7__push.sql, which every other engine shares.
-- Text is NVARCHAR, since VARCHAR holds only the database's code page,
-- under a binary collation: the default one folds case, and a key must
-- keep "A" and "a" apart. A key column cannot change collation once
-- created, so it is declared here rather than converted later.

-- The devices each account is sent push notifications on. Keyed by the hash of
-- the provider and the token, so one phone is on one account at a time: the
-- one that signed in on it last.
CREATE TABLE push_device (
    id NVARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    username NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    provider NVARCHAR(16) COLLATE Latin1_General_100_BIN2 NOT NULL,
    token NVARCHAR(1024) COLLATE Latin1_General_100_BIN2 NOT NULL,
    platform NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX push_device_username ON push_device (username);

-- The Live Activity a customer's phone shows a delivery in, while the delivery lasts.
-- sent_at is when the server last sent it a state, which spaces the updates
-- out: the car reports where it is every few seconds, and the lock screen
-- needs to hear about it far less often than that. ended_at is when the delivery
-- ended, 0 while it lasts: the row is kept until the phone has been told, so an
-- end the push provider did not take is sent again.
CREATE TABLE delivery_activity (
    delivery_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    username NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    activity_id NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    sent_at BIGINT NOT NULL,
    ended_at BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX delivery_activity_ended_at ON delivery_activity (ended_at);

CREATE INDEX delivery_activity_username ON delivery_activity (username);

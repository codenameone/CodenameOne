-- The devices each account is sent push notifications on. Keyed by the hash of
-- the provider and the token, so one phone is on one account at a time: the
-- one that signed in on it last.
CREATE TABLE push_device (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    provider VARCHAR(16) NOT NULL,
    token VARCHAR(1024) NOT NULL,
    platform VARCHAR(32) NOT NULL,
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
    delivery_id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    activity_id VARCHAR(190) NOT NULL,
    sent_at BIGINT NOT NULL,
    ended_at BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX delivery_activity_ended_at ON delivery_activity (ended_at);

CREATE INDEX delivery_activity_username ON delivery_activity (username);

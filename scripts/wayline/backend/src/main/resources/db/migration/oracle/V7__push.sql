-- The Oracle version of ../V7__push.sql, which every other engine shares.
-- Oracle 19c has no BIGINT, INTEGER is NUMBER(38) and DOUBLE PRECISION
-- a decimal float, so the columns are the types the ORM declares there.
-- Text columns are nullable even when the entity requires them: Oracle
-- stores '' as NULL, so a NOT NULL column could never hold an empty
-- string; the ORM enforces 'required' itself and reads NULL back as ''.
-- A DEFAULT comes before NOT NULL, the only order Oracle accepts, and a
-- column named by an Oracle reserved word is quoted.

-- The devices each account is sent push notifications on. Keyed by the hash of
-- the provider and the token, so one phone is on one account at a time: the
-- one that signed in on it last.
CREATE TABLE push_device (
    id VARCHAR2(64 CHAR) NOT NULL PRIMARY KEY,
    username VARCHAR2(190 CHAR),
    provider VARCHAR2(16 CHAR),
    token VARCHAR2(1024 CHAR),
    platform VARCHAR2(32 CHAR),
    updated_at NUMBER(19) NOT NULL
);

CREATE INDEX push_device_username ON push_device (username);

-- The Live Activity a rider's phone shows a ride in, while the ride lasts.
-- sent_at is when the server last sent it a state, which spaces the updates
-- out: the car reports where it is every few seconds, and the lock screen
-- needs to hear about it far less often than that. ended_at is when the ride
-- ended, 0 while it lasts: the row is kept until the phone has been told, so an
-- end the push provider did not take is sent again.
CREATE TABLE ride_activity (
    ride_id VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    username VARCHAR2(190 CHAR),
    activity_id VARCHAR2(190 CHAR),
    sent_at NUMBER(19) NOT NULL,
    ended_at NUMBER(19) DEFAULT 0 NOT NULL
);

CREATE INDEX ride_activity_ended_at ON ride_activity (ended_at);

CREATE INDEX ride_activity_username ON ride_activity (username);

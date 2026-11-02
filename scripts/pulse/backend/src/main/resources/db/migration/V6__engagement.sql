-- Keeping classes full and members coming back.

-- A member saying they will not be in: travelling, injured. No nudge reaches
-- them meanwhile, and with `freeze_billing` their membership is frozen for
-- the days it covers.
CREATE TABLE away_period (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    account VARCHAR(190) NOT NULL,
    starts_on BIGINT NOT NULL,
    ends_on BIGINT NOT NULL,
    reason VARCHAR(16) NOT NULL,
    freeze_billing SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX away_period_account ON away_period (account, ends_on);

-- A campaign an owner sent, and how it did.
CREATE TABLE campaign (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    session_id VARCHAR(32) NOT NULL,
    title VARCHAR(120) NOT NULL,
    body VARCHAR(500) NOT NULL,
    created_by VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL,
    sent INTEGER NOT NULL,
    opened INTEGER NOT NULL,
    booked INTEGER NOT NULL
);

-- One message to one account, from a campaign or from the server's own jobs.
-- Kept so nobody is nudged twice for one thing, and so a booking that follows
-- can be credited to it.
CREATE TABLE nudge (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    account VARCHAR(190) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    campaign_id VARCHAR(32) NOT NULL,
    session_id VARCHAR(32) NOT NULL,
    title VARCHAR(120) NOT NULL,
    body VARCHAR(500) NOT NULL,
    sent_at BIGINT NOT NULL,
    opened_at BIGINT NOT NULL,
    booked_at BIGINT NOT NULL
);
CREATE INDEX nudge_account ON nudge (account, sent_at);
CREATE INDEX nudge_session ON nudge (session_id, account);

-- Two accounts that train together. A request is one row from the asker; it
-- is accepted by writing the row back the other way.
CREATE TABLE friend (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    account VARCHAR(190) NOT NULL,
    friend VARCHAR(190) NOT NULL,
    state VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX friend_account ON friend (account, state);

CREATE TABLE kudos (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    from_account VARCHAR(190) NOT NULL,
    to_account VARCHAR(190) NOT NULL,
    workout_id VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX kudos_once ON kudos (from_account, workout_id);

-- A workout, as the phone's health store or the watch recorded it. Zones are
-- minutes in each of the five heart rate zones, comma separated.
CREATE TABLE workout (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    account VARCHAR(190) NOT NULL,
    session_id VARCHAR(32) NOT NULL,
    activity VARCHAR(32) NOT NULL,
    started_at BIGINT NOT NULL,
    minutes INTEGER NOT NULL,
    avg_hr INTEGER NOT NULL,
    max_hr INTEGER NOT NULL,
    calories INTEGER NOT NULL,
    zones VARCHAR(64) NOT NULL,
    source VARCHAR(16) NOT NULL,
    shared SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX workout_account ON workout (account, started_at);
CREATE INDEX workout_session ON workout (session_id);

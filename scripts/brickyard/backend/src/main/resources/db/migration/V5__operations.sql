-- Seasons, product edits, the network half of the referral check and the
-- daily guard of the scheduled jobs. Portable SQL only, as in V1 and V3:
-- string keys, plain integers, epoch-millisecond times. A column added to a
-- table that has rows carries a constant default, which SQLite needs.

CREATE TABLE season (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    starts_at BIGINT NOT NULL,
    ends_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX season_starts_at ON season (starts_at);

-- The season a row's season XP and premium pass belong to; '' is the season
-- before any was started. A row from an older season is reset on its next read.
ALTER TABLE progress ADD COLUMN season_id VARCHAR(32) NOT NULL DEFAULT '';

ALTER TABLE product ADD COLUMN active INTEGER NOT NULL DEFAULT 1;

ALTER TABLE invite ADD COLUMN ip_hash VARCHAR(64) NOT NULL DEFAULT '';
ALTER TABLE referral ADD COLUMN ip_hash VARCHAR(64) NOT NULL DEFAULT '';

CREATE INDEX payout_request_username ON payout_request (username);
CREATE INDEX sanction_until_at ON sanction (until_at);

-- One row per scheduled job per UTC day it ran, so a job is paid out once a
-- day however many instances or restarts there are.
CREATE TABLE job_run (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    job VARCHAR(32) NOT NULL,
    day BIGINT NOT NULL,
    detail VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL
);

-- Every script here is written once for SQLite, PostgreSQL and MySQL: no
-- generated keys, no native boolean or timestamp. A yes/no is a SMALLINT 0/1,
-- a time is milliseconds since the epoch in a BIGINT, and a list is a
-- comma-separated VARCHAR.

-- An ad campaign, its creative and its budget.
CREATE TABLE ad_campaign (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    advertiser VARCHAR(120) NOT NULL,
    name VARCHAR(120) NOT NULL,
    creative_url VARCHAR(255) NOT NULL,
    duration_seconds INTEGER NOT NULL,
    skippable_after_seconds INTEGER NOT NULL,
    click_url VARCHAR(255) NOT NULL,
    cpm_cents BIGINT NOT NULL,
    budget_cents BIGINT NOT NULL,
    spent_millicents BIGINT NOT NULL,
    impressions BIGINT NOT NULL,
    clicks BIGINT NOT NULL,
    genres VARCHAR(255) NOT NULL,
    active SMALLINT NOT NULL,
    starts_at BIGINT NOT NULL,
    ends_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);

-- An ad tracking event.
CREATE TABLE ad_event (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    campaign_id VARCHAR(32) NOT NULL,
    session_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    event VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX ad_event_campaign_id_created_at ON ad_event (campaign_id, created_at);

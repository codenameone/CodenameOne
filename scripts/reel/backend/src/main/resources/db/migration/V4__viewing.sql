-- Every script here is written once for SQLite, PostgreSQL and MySQL: no
-- generated keys, no native boolean or timestamp. A yes/no is a SMALLINT 0/1,
-- a time is milliseconds since the epoch in a BIGINT, and a list is a
-- comma-separated VARCHAR.

-- A stream: who plays what where, and the lease its heartbeats renew.
CREATE TABLE playback (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    profile_id VARCHAR(32) NOT NULL,
    title_id VARCHAR(32) NOT NULL,
    video_id VARCHAR(32) NOT NULL,
    device_id VARCHAR(64) NOT NULL,
    device_name VARCHAR(80) NOT NULL,
    ip VARCHAR(64) NOT NULL,
    country VARCHAR(8) NOT NULL,
    scheme VARCHAR(16) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    started_at BIGINT NOT NULL,
    last_heartbeat_at BIGINT NOT NULL,
    position_seconds INTEGER NOT NULL,
    ended_at BIGINT NOT NULL,
    ended_reason VARCHAR(32) NOT NULL
);
CREATE INDEX playback_username_ended_at ON playback (username, ended_at);
CREATE INDEX playback_ended_at_last_heartbeat_at ON playback (ended_at, last_heartbeat_at);
CREATE INDEX playback_started_at ON playback (started_at);

-- Where a profile is in a video. The id is the profile id and the video id.
CREATE TABLE progress (
    id VARCHAR(80) NOT NULL PRIMARY KEY,
    profile_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    title_id VARCHAR(32) NOT NULL,
    video_id VARCHAR(32) NOT NULL,
    position_seconds INTEGER NOT NULL,
    duration_seconds INTEGER NOT NULL,
    completed SMALLINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX progress_profile_id_updated_at ON progress (profile_id, updated_at);
CREATE INDEX progress_title_id ON progress (title_id);

-- A title on a profile's list. The id is the profile id and the title id.
CREATE TABLE list_entry (
    id VARCHAR(80) NOT NULL PRIMARY KEY,
    profile_id VARCHAR(32) NOT NULL,
    title_id VARCHAR(32) NOT NULL,
    added_at BIGINT NOT NULL
);
CREATE INDEX list_entry_profile_id ON list_entry (profile_id);

-- A profile's rating of a title. The id is the profile id and the title id.
CREATE TABLE title_rating (
    id VARCHAR(80) NOT NULL PRIMARY KEY,
    profile_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    title_id VARCHAR(32) NOT NULL,
    stars INTEGER NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX title_rating_title_id ON title_rating (title_id);

-- A title licensed for offline play on one device.
CREATE TABLE download (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    device_id VARCHAR(64) NOT NULL,
    title_id VARCHAR(32) NOT NULL,
    video_id VARCHAR(32) NOT NULL,
    scheme VARCHAR(16) NOT NULL,
    license_expires_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX download_username ON download (username);

-- A viewer's answer to an interaction.
CREATE TABLE answer (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    interaction_id VARCHAR(32) NOT NULL,
    video_id VARCHAR(32) NOT NULL,
    title_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    profile_id VARCHAR(32) NOT NULL,
    choices VARCHAR(64) NOT NULL,
    correct SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX answer_interaction_id_username ON answer (interaction_id, username);
CREATE INDEX answer_title_id_username ON answer (title_id, username);

-- A course completion certificate.
CREATE TABLE certificate (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    title_id VARCHAR(32) NOT NULL,
    holder_name VARCHAR(120) NOT NULL,
    score_percent INTEGER NOT NULL,
    issued_at BIGINT NOT NULL,
    verify_code VARCHAR(16) NOT NULL
);
CREATE INDEX certificate_username ON certificate (username);
CREATE INDEX certificate_verify_code ON certificate (verify_code);

-- A sign an account is used beyond its household.
CREATE TABLE sharing_alert (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    detail VARCHAR(255) NOT NULL,
    distinct_ips INTEGER NOT NULL,
    distinct_countries INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL,
    decided_by VARCHAR(190) NOT NULL,
    note VARCHAR(255) NOT NULL
);
CREATE INDEX sharing_alert_status_created_at ON sharing_alert (status, created_at);
CREATE INDEX sharing_alert_username ON sharing_alert (username);

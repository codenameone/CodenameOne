-- Every script here is written once for SQLite, PostgreSQL and MySQL: no
-- generated keys, no native boolean or timestamp. A yes/no is a SMALLINT 0/1,
-- a time is milliseconds since the epoch in a BIGINT, and a list is a
-- comma-separated VARCHAR.

-- A resumable upload of a video's source file.
CREATE TABLE upload (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    video_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    file_name VARCHAR(200) NOT NULL,
    total_bytes BIGINT NOT NULL,
    received_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX upload_video_id ON upload (video_id);

-- A packaging job a worker claims, with the lease it holds it under.
CREATE TABLE job (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    video_id VARCHAR(32) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    progress_percent INTEGER NOT NULL,
    worker VARCHAR(64) NOT NULL,
    error VARCHAR(500) NOT NULL,
    attempts INTEGER NOT NULL,
    created_at BIGINT NOT NULL,
    started_at BIGINT NOT NULL,
    finished_at BIGINT NOT NULL,
    lease_until BIGINT NOT NULL
);
CREATE INDEX job_status_created_at ON job (status, created_at);
CREATE INDEX job_video_id ON job (video_id);

-- A banned frame hash or keyword.
CREATE TABLE blocked_item (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    kind VARCHAR(16) NOT NULL,
    value VARCHAR(190) NOT NULL,
    note VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL,
    created_by VARCHAR(190) NOT NULL
);
CREATE INDEX blocked_item_kind ON blocked_item (kind);

-- Something waiting for a moderator: a title, a report or an appeal.
CREATE TABLE moderation_item (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    kind VARCHAR(16) NOT NULL,
    subject_id VARCHAR(32) NOT NULL,
    owner VARCHAR(190) NOT NULL,
    reason VARCHAR(64) NOT NULL,
    detail VARCHAR(1000) NOT NULL,
    signals VARCHAR(500) NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    status VARCHAR(16) NOT NULL,
    decided_by VARCHAR(190) NOT NULL,
    decision VARCHAR(16) NOT NULL,
    note VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL,
    decided_at BIGINT NOT NULL
);
CREATE INDEX moderation_item_status_created_at ON moderation_item (status, created_at);
CREATE INDEX moderation_item_subject_id ON moderation_item (subject_id);

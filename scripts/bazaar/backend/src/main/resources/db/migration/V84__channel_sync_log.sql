-- What the sync did per channel, newest first in the back office.
CREATE TABLE channel_sync_log (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    channel_id VARCHAR(32) NOT NULL,
    action VARCHAR(16) NOT NULL,
    ref VARCHAR(64) NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX channel_sync_log_channel ON channel_sync_log (channel_id, created_at);

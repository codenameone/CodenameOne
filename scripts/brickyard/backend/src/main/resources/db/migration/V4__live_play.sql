-- Rooms and tickets that every backend instance can see, per-account
-- settings (avatar colours, parental limits) and push devices. Portable SQL
-- only, as in V1 and V3: string keys, plain integers, epoch-millisecond times.

CREATE TABLE play_server (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    experience_id VARCHAR(32) NOT NULL,
    instance_id VARCHAR(64) NOT NULL,
    socket_url VARCHAR(512) NOT NULL,
    seats INTEGER NOT NULL,
    max_players INTEGER NOT NULL,
    private_room INTEGER NOT NULL,
    heartbeat BIGINT NOT NULL
);
CREATE INDEX play_server_experience_id ON play_server (experience_id);
CREATE INDEX play_server_heartbeat ON play_server (heartbeat);

CREATE TABLE play_ticket (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    server_id VARCHAR(64) NOT NULL,
    expires BIGINT NOT NULL
);
CREATE INDEX play_ticket_expires ON play_ticket (expires);

CREATE TABLE account_settings (
    id VARCHAR(190) NOT NULL PRIMARY KEY,
    avatar VARCHAR(200) NOT NULL,
    spend_limit BIGINT NOT NULL,
    spent BIGINT NOT NULL,
    spent_month BIGINT NOT NULL,
    chat_enabled INTEGER NOT NULL
);

CREATE TABLE push_device (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    provider VARCHAR(16) NOT NULL,
    token VARCHAR(1024) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX push_device_username ON push_device (username);

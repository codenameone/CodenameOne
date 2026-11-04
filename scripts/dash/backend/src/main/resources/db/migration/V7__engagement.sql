-- Engagement pipelines: a query over the players and an action, run every
-- hour, and when each one last acted on each player. Portable, like V6.

CREATE TABLE pipeline (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    name VARCHAR(40) NOT NULL,
    enabled SMALLINT NOT NULL,
    conditions VARCHAR(500) NOT NULL,
    action VARCHAR(10) NOT NULL,
    title VARCHAR(60) NOT NULL,
    body VARCHAR(200) NOT NULL,
    screen VARCHAR(20) NOT NULL,
    amount INTEGER NOT NULL,
    cooldown_days INTEGER NOT NULL,
    reached INTEGER NOT NULL,
    last_run BIGINT NOT NULL,
    created_by VARCHAR(190) NOT NULL
);

CREATE TABLE pipeline_hit (
    id VARCHAR(230) NOT NULL PRIMARY KEY,
    pipeline VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    hit_at BIGINT NOT NULL
);

CREATE INDEX pipeline_hit_by_pipeline ON pipeline_hit (pipeline);

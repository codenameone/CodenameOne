-- An automation run paused at a wait.minutes step, resumed by a scheduled job.
CREATE TABLE automation_run (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    automation_id VARCHAR(32) NOT NULL,
    store_id VARCHAR(32) NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    context TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    resume_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX automation_run_status_resume_at ON automation_run (status, resume_at);

-- The workforce rules a real dispatch desk plans by: certifications with
-- their expiry, the territories a technician covers, daily caps beside the
-- weekly ones, a job's SLA deadline, the certifications it needs, the
-- technicians a customer will not have, and the job it has to wait for. The
-- company-wide rules -- the meal break and how the planner weighs what it
-- trades off -- live in a single planner_policy row.

ALTER TABLE technician ADD COLUMN certifications VARCHAR(500) DEFAULT '' NOT NULL;
ALTER TABLE technician ADD COLUMN territories VARCHAR(120) DEFAULT '' NOT NULL;
ALTER TABLE technician ADD COLUMN max_jobs_per_day INTEGER DEFAULT 8 NOT NULL;
ALTER TABLE technician ADD COLUMN max_minutes_per_day INTEGER DEFAULT 600 NOT NULL;

ALTER TABLE job ADD COLUMN due_at BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE job ADD COLUMN certifications VARCHAR(255) DEFAULT '' NOT NULL;
ALTER TABLE job ADD COLUMN excluded_techs VARCHAR(255) DEFAULT '' NOT NULL;
ALTER TABLE job ADD COLUMN after_job_id VARCHAR(40) DEFAULT '' NOT NULL;
ALTER TABLE job ADD COLUMN unassigned_reason VARCHAR(160) DEFAULT '' NOT NULL;

CREATE TABLE planner_policy (
    id VARCHAR(40) NOT NULL PRIMARY KEY,
    break_minutes INTEGER NOT NULL,
    break_after_minutes INTEGER NOT NULL,
    break_before_minutes INTEGER NOT NULL,
    strict_territories SMALLINT NOT NULL,
    travel_weight INTEGER NOT NULL,
    overtime_weight INTEGER NOT NULL,
    lateness_weight INTEGER NOT NULL,
    continuity_weight INTEGER NOT NULL,
    balance_weight INTEGER NOT NULL,
    territory_weight INTEGER NOT NULL,
    urgent_sla_hours INTEGER NOT NULL,
    routine_sla_hours INTEGER NOT NULL,
    updated_at BIGINT NOT NULL
);

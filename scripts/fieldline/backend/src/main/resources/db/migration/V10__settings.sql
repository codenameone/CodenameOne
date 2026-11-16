-- Company settings beyond the planner's weights: rush hours, when the clock
-- rounds and closes itself, the labour rates a technician is paid at, the
-- hours they clock, and the meal breaks the last plan placed, which the
-- dispatch board draws.

ALTER TABLE planner_policy ADD COLUMN rush_am_from INTEGER DEFAULT 420 NOT NULL;
ALTER TABLE planner_policy ADD COLUMN rush_am_to INTEGER DEFAULT 570 NOT NULL;
ALTER TABLE planner_policy ADD COLUMN rush_pm_from INTEGER DEFAULT 960 NOT NULL;
ALTER TABLE planner_policy ADD COLUMN rush_pm_to INTEGER DEFAULT 1110 NOT NULL;
ALTER TABLE planner_policy ADD COLUMN rush_extra_pct INTEGER DEFAULT 25 NOT NULL;
ALTER TABLE planner_policy ADD COLUMN time_rounding_minutes INTEGER DEFAULT 15 NOT NULL;
ALTER TABLE planner_policy ADD COLUMN auto_clock_out_hours INTEGER DEFAULT 12 NOT NULL;

ALTER TABLE technician ADD COLUMN rate_id VARCHAR(40) DEFAULT '' NOT NULL;

CREATE TABLE labour_rate (
    id VARCHAR(40) NOT NULL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    hourly_cents INTEGER NOT NULL,
    overtime_pct INTEGER NOT NULL,
    weekend_pct INTEGER NOT NULL,
    callout_cents INTEGER NOT NULL
);

CREATE TABLE time_entry (
    id VARCHAR(40) NOT NULL PRIMARY KEY,
    tech_id VARCHAR(40) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    start_at BIGINT NOT NULL,
    end_at BIGINT NOT NULL,
    job_id VARCHAR(40) NOT NULL
);

CREATE INDEX time_entry_tech ON time_entry (tech_id, start_at);

CREATE TABLE planned_break (
    id VARCHAR(40) NOT NULL PRIMARY KEY,
    tech_id VARCHAR(40) NOT NULL,
    start_at BIGINT NOT NULL,
    end_at BIGINT NOT NULL
);

-- The field: technicians and their weeks, sites, jobs and their checklists,
-- and every run of the scheduler with what it changed. Times are epoch
-- milliseconds, except a work window's, which are minutes into a weekday.

CREATE TABLE technician (
    id VARCHAR(40) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    name VARCHAR(120) NOT NULL,
    initials VARCHAR(4) NOT NULL,
    color INTEGER NOT NULL,
    skills VARCHAR(255) NOT NULL,
    home_lat DOUBLE PRECISION NOT NULL,
    home_lng DOUBLE PRECISION NOT NULL,
    home_label VARCHAR(255) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    seen_at BIGINT NOT NULL,
    max_drive_minutes INTEGER NOT NULL,
    regular_minutes_per_week INTEGER NOT NULL,
    max_minutes_per_week INTEGER NOT NULL,
    active SMALLINT NOT NULL
);

CREATE INDEX technician_username ON technician (username);

CREATE TABLE work_window (
    id VARCHAR(40) NOT NULL PRIMARY KEY,
    tech_id VARCHAR(40) NOT NULL,
    weekday INTEGER NOT NULL,
    start_minute INTEGER NOT NULL,
    end_minute INTEGER NOT NULL
);

CREATE INDEX work_window_tech ON work_window (tech_id);

CREATE TABLE time_off (
    id VARCHAR(40) NOT NULL PRIMARY KEY,
    tech_id VARCHAR(40) NOT NULL,
    start_at BIGINT NOT NULL,
    end_at BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE INDEX time_off_tech ON time_off (tech_id, start_at);
CREATE INDEX time_off_status ON time_off (status);

CREATE TABLE site (
    id VARCHAR(40) NOT NULL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    address VARCHAR(255) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    customer VARCHAR(160) NOT NULL,
    contact_phone VARCHAR(32) NOT NULL,
    asset_type VARCHAR(64) NOT NULL,
    asset_model VARCHAR(120) NOT NULL
);

CREATE TABLE job (
    id VARCHAR(40) NOT NULL PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    priority INTEGER NOT NULL,
    site_id VARCHAR(40) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    skills VARCHAR(255) NOT NULL,
    window_start BIGINT NOT NULL,
    window_end BIGINT NOT NULL,
    tech_id VARCHAR(40) NOT NULL,
    previous_tech_id VARCHAR(40) NOT NULL,
    planned_start BIGINT NOT NULL,
    planned_end BIGINT NOT NULL,
    travel_before INTEGER NOT NULL,
    sequence INTEGER NOT NULL,
    actual_start BIGINT NOT NULL,
    actual_end BIGINT NOT NULL,
    notes VARCHAR(2000) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX job_tech_start ON job (tech_id, planned_start);
CREATE INDEX job_window ON job (window_end);

CREATE TABLE job_check (
    id VARCHAR(48) NOT NULL PRIMARY KEY,
    job_id VARCHAR(40) NOT NULL,
    ordinal INTEGER NOT NULL,
    label VARCHAR(255) NOT NULL,
    result VARCHAR(8) NOT NULL,
    note VARCHAR(500) NOT NULL
);

CREATE INDEX job_check_job ON job_check (job_id, ordinal);

CREATE TABLE schedule_run (
    id VARCHAR(40) NOT NULL PRIMARY KEY,
    ran_at BIGINT NOT NULL,
    kind VARCHAR(16) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    before_kpis VARCHAR(120) NOT NULL,
    after_kpis VARCHAR(120) NOT NULL,
    operators VARCHAR(500) NOT NULL,
    iterations INTEGER NOT NULL,
    millis BIGINT NOT NULL,
    actor VARCHAR(190) NOT NULL
);

CREATE INDEX schedule_run_ran_at ON schedule_run (ran_at);

CREATE TABLE schedule_change (
    id VARCHAR(48) NOT NULL PRIMARY KEY,
    run_id VARCHAR(40) NOT NULL,
    job_id VARCHAR(40) NOT NULL,
    job_title VARCHAR(160) NOT NULL,
    from_tech VARCHAR(40) NOT NULL,
    to_tech VARCHAR(40) NOT NULL,
    from_start BIGINT NOT NULL,
    to_start BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL
);

CREATE INDEX schedule_change_run ON schedule_change (run_id);

CREATE TABLE manual_chunk (
    id VARCHAR(40) NOT NULL PRIMARY KEY,
    asset_type VARCHAR(64) NOT NULL,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    terms VARCHAR(2000) NOT NULL
);

CREATE INDEX manual_chunk_asset ON manual_chunk (asset_type);

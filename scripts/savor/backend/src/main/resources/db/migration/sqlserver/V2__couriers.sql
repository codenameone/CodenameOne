-- The SQL Server version of ../V2__couriers.sql, which every other engine shares.
-- Text is NVARCHAR, since VARCHAR holds only the database's code page,
-- under a binary collation: the default one folds case, and a key must
-- keep "A" and "a" apart. A key column cannot change collation once
-- created, so it is declared here rather than converted later.

-- A courier's live state: on line or not, and where the car was last seen.
CREATE TABLE courier_state (
    username NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    online SMALLINT NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    heading DOUBLE PRECISION NOT NULL,
    last_seen BIGINT NOT NULL,
    rating_sum BIGINT NOT NULL,
    rating_count BIGINT NOT NULL
);
-- Matching asks for the couriers on line inside a latitude band.
CREATE INDEX courier_online ON courier_state (online, lat);

-- An application to deliver: one per account, edited as a draft, handed in, and
-- then approved or rejected by an admin. The COURIER role follows `approved`.
CREATE TABLE courier_application (
    username NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    status NVARCHAR(16) COLLATE Latin1_General_100_BIN2 NOT NULL,
    legal_name NVARCHAR(120) COLLATE Latin1_General_100_BIN2 NOT NULL,
    date_of_birth NVARCHAR(10) COLLATE Latin1_General_100_BIN2 NOT NULL,
    licence_number NVARCHAR(40) COLLATE Latin1_General_100_BIN2 NOT NULL,
    licence_expiry NVARCHAR(10) COLLATE Latin1_General_100_BIN2 NOT NULL,
    vehicle_make NVARCHAR(40) COLLATE Latin1_General_100_BIN2 NOT NULL,
    vehicle_model NVARCHAR(40) COLLATE Latin1_General_100_BIN2 NOT NULL,
    vehicle_year INTEGER NOT NULL,
    vehicle_color NVARCHAR(24) COLLATE Latin1_General_100_BIN2 NOT NULL,
    vehicle_plate NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    rejection_reason NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL,
    created_at BIGINT NOT NULL,
    submitted_at BIGINT NOT NULL,
    reviewed_at BIGINT NOT NULL,
    reviewed_by NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL
);
CREATE INDEX courier_application_status ON courier_application (status, submitted_at);

-- How long a courier was on line on each day (days since the epoch, UTC), added
-- to by every position report.
CREATE TABLE courier_hours (
    username NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    day_number BIGINT NOT NULL,
    millis BIGINT NOT NULL,
    PRIMARY KEY (username, day_number)
);

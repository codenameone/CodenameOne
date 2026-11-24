-- The SQL Server version of ../V2__drivers.sql, which every other engine shares.
-- Text is NVARCHAR, since VARCHAR holds only the database's code page,
-- under a binary collation: the default one folds case, and a key must
-- keep "A" and "a" apart. A key column cannot change collation once
-- created, so it is declared here rather than converted later.

-- A driver's live state: on line or not, and where the car was last seen.
CREATE TABLE driver_state (
    username NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    online SMALLINT NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    heading DOUBLE PRECISION NOT NULL,
    last_seen BIGINT NOT NULL,
    rating_sum BIGINT NOT NULL,
    rating_count BIGINT NOT NULL
);
-- Matching asks for the drivers on line inside a latitude band.
CREATE INDEX driver_online ON driver_state (online, lat);

-- An application to drive: one per account, edited as a draft, handed in, and
-- then approved or rejected by an admin. The DRIVER role follows `approved`,
-- and matching reads the car's product from here.
CREATE TABLE driver_application (
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
    vehicle_seats INTEGER NOT NULL,
    product NVARCHAR(16) COLLATE Latin1_General_100_BIN2 NOT NULL,
    -- Whether the car takes a wheelchair. Not called "accessible", which MySQL
    -- reserves.
    wheelchair SMALLINT NOT NULL,
    rejection_reason NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL,
    created_at BIGINT NOT NULL,
    submitted_at BIGINT NOT NULL,
    reviewed_at BIGINT NOT NULL,
    reviewed_by NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pets SMALLINT NOT NULL
);
CREATE INDEX driver_application_status ON driver_application (status, submitted_at);

-- How long a driver was on line on each day (days since the epoch, UTC), added
-- to by every position report.
CREATE TABLE driver_hours (
    username NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    day_number BIGINT NOT NULL,
    millis BIGINT NOT NULL,
    PRIMARY KEY (username, day_number)
);

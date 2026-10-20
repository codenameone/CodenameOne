-- A courier's live state: on line or not, and where the car was last seen.
CREATE TABLE courier_state (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
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
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    status VARCHAR(16) NOT NULL,
    legal_name VARCHAR(120) NOT NULL,
    date_of_birth VARCHAR(10) NOT NULL,
    licence_number VARCHAR(40) NOT NULL,
    licence_expiry VARCHAR(10) NOT NULL,
    vehicle_make VARCHAR(40) NOT NULL,
    vehicle_model VARCHAR(40) NOT NULL,
    vehicle_year INTEGER NOT NULL,
    vehicle_color VARCHAR(24) NOT NULL,
    vehicle_plate VARCHAR(32) NOT NULL,
    rejection_reason VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL,
    submitted_at BIGINT NOT NULL,
    reviewed_at BIGINT NOT NULL,
    reviewed_by VARCHAR(190) NOT NULL
);
CREATE INDEX courier_application_status ON courier_application (status, submitted_at);

-- How long a courier was on line on each day (days since the epoch, UTC), added
-- to by every position report.
CREATE TABLE courier_hours (
    username VARCHAR(190) NOT NULL,
    day_number BIGINT NOT NULL,
    millis BIGINT NOT NULL,
    PRIMARY KEY (username, day_number)
);

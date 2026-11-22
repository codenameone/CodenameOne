-- The Oracle version of ../V2__drivers.sql, which every other engine shares.
-- Oracle 19c has no BIGINT, INTEGER is NUMBER(38) and DOUBLE PRECISION
-- a decimal float, so the columns are the types the ORM declares there.
-- Text columns are nullable even when the entity requires them: Oracle
-- stores '' as NULL, so a NOT NULL column could never hold an empty
-- string; the ORM enforces 'required' itself and reads NULL back as ''.
-- A DEFAULT comes before NOT NULL, the only order Oracle accepts, and a
-- column named by an Oracle reserved word is quoted.

-- A driver's live state: on line or not, and where the car was last seen.
CREATE TABLE driver_state (
    username VARCHAR2(190 CHAR) NOT NULL PRIMARY KEY,
    "ONLINE" NUMBER(5) NOT NULL,
    lat BINARY_DOUBLE NOT NULL,
    lng BINARY_DOUBLE NOT NULL,
    heading BINARY_DOUBLE NOT NULL,
    last_seen NUMBER(19) NOT NULL,
    rating_sum NUMBER(19) NOT NULL,
    rating_count NUMBER(19) NOT NULL
);
-- Matching asks for the drivers on line inside a latitude band.
CREATE INDEX driver_online ON driver_state ("ONLINE", lat);

-- An application to drive: one per account, edited as a draft, handed in, and
-- then approved or rejected by an admin. The DRIVER role follows `approved`,
-- and matching reads the car's product from here.
CREATE TABLE driver_application (
    username VARCHAR2(190 CHAR) NOT NULL PRIMARY KEY,
    status VARCHAR2(16 CHAR),
    legal_name VARCHAR2(120 CHAR),
    date_of_birth VARCHAR2(10 CHAR),
    licence_number VARCHAR2(40 CHAR),
    licence_expiry VARCHAR2(10 CHAR),
    vehicle_make VARCHAR2(40 CHAR),
    vehicle_model VARCHAR2(40 CHAR),
    vehicle_year NUMBER(10) NOT NULL,
    vehicle_color VARCHAR2(24 CHAR),
    vehicle_plate VARCHAR2(32 CHAR),
    vehicle_seats NUMBER(10) NOT NULL,
    product VARCHAR2(16 CHAR),
    -- Whether the car takes a wheelchair. Not called "accessible", which MySQL
    -- reserves.
    wheelchair NUMBER(5) NOT NULL,
    rejection_reason VARCHAR2(255 CHAR),
    created_at NUMBER(19) NOT NULL,
    submitted_at NUMBER(19) NOT NULL,
    reviewed_at NUMBER(19) NOT NULL,
    reviewed_by VARCHAR2(190 CHAR),
    pets NUMBER(5) NOT NULL
);
CREATE INDEX driver_application_status ON driver_application (status, submitted_at);

-- How long a driver was on line on each day (days since the epoch, UTC), added
-- to by every position report.
CREATE TABLE driver_hours (
    username VARCHAR2(190 CHAR),
    day_number NUMBER(19) NOT NULL,
    millis NUMBER(19) NOT NULL,
    PRIMARY KEY (username, day_number)
);

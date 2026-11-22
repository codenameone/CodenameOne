-- The Oracle version of ../V4__rides.sql, which every other engine shares.
-- Oracle 19c has no BIGINT, INTEGER is NUMBER(38) and DOUBLE PRECISION
-- a decimal float, so the columns are the types the ORM declares there.
-- Text columns are nullable even when the entity requires them: Oracle
-- stores '' as NULL, so a NOT NULL column could never hold an empty
-- string; the ORM enforces 'required' itself and reads NULL back as ''.
-- A DEFAULT comes before NOT NULL, the only order Oracle accepts, and a
-- column named by an Oracle reserved word is quoted.

CREATE TABLE ride (
    id VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    state VARCHAR2(24 CHAR),
    rider VARCHAR2(190 CHAR),
    -- The driver it is offered to, then the driver who took it. Empty until one
    -- is found, and again between one driver passing and the next being asked.
    driver VARCHAR2(190 CHAR),
    pickup_lat BINARY_DOUBLE NOT NULL,
    pickup_lng BINARY_DOUBLE NOT NULL,
    pickup_address VARCHAR2(255 CHAR),
    dropoff_lat BINARY_DOUBLE NOT NULL,
    dropoff_lng BINARY_DOUBLE NOT NULL,
    dropoff_address VARCHAR2(255 CHAR),
    fare_cents NUMBER(19) NOT NULL,
    currency VARCHAR2(3 CHAR),
    distance_meters BINARY_DOUBLE NOT NULL,
    duration_seconds BINARY_DOUBLE NOT NULL,
    requested_at NUMBER(19) NOT NULL,
    updated_at NUMBER(19) NOT NULL,
    -- When the offer in hand lapses, or when the search for a driver gives up.
    offer_expires_at NUMBER(19) NOT NULL,
    search_until NUMBER(19) NOT NULL,
    stars NUMBER(5) NOT NULL,
    payment_status VARCHAR2(16 CHAR),
    -- What the rider asked for beyond two points, how the ride is paid for, and
    -- the parts its price is made of. Every amount is cents, fixed when the ride is
    -- asked for; the tip and the driver's share are written when they are known.
    product VARCHAR2(16 CHAR),
    method_id VARCHAR2(32 CHAR),
    method_label VARCHAR2(64 CHAR),
    base_cents NUMBER(19) NOT NULL,
    distance_cents NUMBER(19) NOT NULL,
    time_cents NUMBER(19) NOT NULL,
    fee_cents NUMBER(19) NOT NULL,
    tip_cents NUMBER(19) NOT NULL,
    driver_cents NUMBER(19) NOT NULL,
    women_only NUMBER(5) NOT NULL,
    quiet NUMBER(5) NOT NULL,
    wheelchair NUMBER(5) NOT NULL,
    note VARCHAR2(255 CHAR),
    -- A rider may ask for a car their pet is welcome in, and a driver says whether
    -- theirs is one. Neither is, until someone says so.
    pets NUMBER(5) NOT NULL
);
CREATE INDEX ride_rider ON ride (rider, requested_at);
CREATE INDEX ride_driver ON ride (driver, requested_at);
CREATE INDEX ride_state ON ride (state);

-- Every change of a ride's state, for the admin's view of what happened.
CREATE TABLE ride_event (
    id VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    ride_id VARCHAR2(32 CHAR),
    state VARCHAR2(24 CHAR),
    actor VARCHAR2(190 CHAR),
    created_at NUMBER(19) NOT NULL
);
CREATE INDEX ride_event_ride ON ride_event (ride_id, created_at);

-- The drivers who passed on a ride, or let its offer lapse: they are not asked
-- again for the same ride.
CREATE TABLE ride_passed (
    ride_id VARCHAR2(32 CHAR),
    driver VARCHAR2(190 CHAR),
    PRIMARY KEY (ride_id, driver)
);

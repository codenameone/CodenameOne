-- The SQL Server version of ../V4__rides.sql, which every other engine shares.
-- Text is NVARCHAR, since VARCHAR holds only the database's code page,
-- under a binary collation: the default one folds case, and a key must
-- keep "A" and "a" apart. A key column cannot change collation once
-- created, so it is declared here rather than converted later.

CREATE TABLE ride (
    id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    state NVARCHAR(24) COLLATE Latin1_General_100_BIN2 NOT NULL,
    rider NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    -- The driver it is offered to, then the driver who took it. Empty until one
    -- is found, and again between one driver passing and the next being asked.
    driver NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pickup_lat DOUBLE PRECISION NOT NULL,
    pickup_lng DOUBLE PRECISION NOT NULL,
    pickup_address NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL,
    dropoff_lat DOUBLE PRECISION NOT NULL,
    dropoff_lng DOUBLE PRECISION NOT NULL,
    dropoff_address NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL,
    fare_cents BIGINT NOT NULL,
    currency NVARCHAR(3) COLLATE Latin1_General_100_BIN2 NOT NULL,
    distance_meters DOUBLE PRECISION NOT NULL,
    duration_seconds DOUBLE PRECISION NOT NULL,
    requested_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    -- When the offer in hand lapses, or when the search for a driver gives up.
    offer_expires_at BIGINT NOT NULL,
    search_until BIGINT NOT NULL,
    stars SMALLINT NOT NULL,
    payment_status NVARCHAR(16) COLLATE Latin1_General_100_BIN2 NOT NULL,
    -- What the rider asked for beyond two points, how the ride is paid for, and
    -- the parts its price is made of. Every amount is cents, fixed when the ride is
    -- asked for; the tip and the driver's share are written when they are known.
    product NVARCHAR(16) COLLATE Latin1_General_100_BIN2 NOT NULL,
    method_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    method_label NVARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL,
    base_cents BIGINT NOT NULL,
    distance_cents BIGINT NOT NULL,
    time_cents BIGINT NOT NULL,
    fee_cents BIGINT NOT NULL,
    tip_cents BIGINT NOT NULL,
    driver_cents BIGINT NOT NULL,
    women_only SMALLINT NOT NULL,
    quiet SMALLINT NOT NULL,
    wheelchair SMALLINT NOT NULL,
    note NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL,
    -- A rider may ask for a car their pet is welcome in, and a driver says whether
    -- theirs is one. Neither is, until someone says so.
    pets SMALLINT NOT NULL
);
CREATE INDEX ride_rider ON ride (rider, requested_at);
CREATE INDEX ride_driver ON ride (driver, requested_at);
CREATE INDEX ride_state ON ride (state);

-- Every change of a ride's state, for the admin's view of what happened.
CREATE TABLE ride_event (
    id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    ride_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    state NVARCHAR(24) COLLATE Latin1_General_100_BIN2 NOT NULL,
    actor NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX ride_event_ride ON ride_event (ride_id, created_at);

-- The drivers who passed on a ride, or let its offer lapse: they are not asked
-- again for the same ride.
CREATE TABLE ride_passed (
    ride_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    driver NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    PRIMARY KEY (ride_id, driver)
);

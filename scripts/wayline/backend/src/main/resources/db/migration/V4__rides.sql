CREATE TABLE wl_ride (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    state VARCHAR(24) NOT NULL,
    rider VARCHAR(190) NOT NULL,
    -- The driver it is offered to, then the driver who took it. Empty until one
    -- is found, and again between one driver passing and the next being asked.
    driver VARCHAR(190) NOT NULL,
    pickup_lat DOUBLE PRECISION NOT NULL,
    pickup_lng DOUBLE PRECISION NOT NULL,
    pickup_address VARCHAR(255) NOT NULL,
    dropoff_lat DOUBLE PRECISION NOT NULL,
    dropoff_lng DOUBLE PRECISION NOT NULL,
    dropoff_address VARCHAR(255) NOT NULL,
    fare_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    distance_m DOUBLE PRECISION NOT NULL,
    duration_s DOUBLE PRECISION NOT NULL,
    requested_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    -- When the offer in hand lapses, or when the search for a driver gives up.
    offer_expires_at BIGINT NOT NULL,
    search_until BIGINT NOT NULL,
    stars SMALLINT NOT NULL,
    payment_status VARCHAR(16) NOT NULL,
    -- What the rider asked for beyond two points, how the ride is paid for, and
    -- the parts its price is made of. Every amount is cents, fixed when the ride is
    -- asked for; the tip and the driver's share are written when they are known.
    product VARCHAR(16) NOT NULL,
    method_id VARCHAR(32) NOT NULL,
    method_label VARCHAR(64) NOT NULL,
    base_cents BIGINT NOT NULL,
    distance_cents BIGINT NOT NULL,
    time_cents BIGINT NOT NULL,
    fee_cents BIGINT NOT NULL,
    tip_cents BIGINT NOT NULL,
    driver_cents BIGINT NOT NULL,
    women_only SMALLINT NOT NULL,
    quiet SMALLINT NOT NULL,
    wheelchair SMALLINT NOT NULL,
    note VARCHAR(255) NOT NULL,
    -- A rider may ask for a car their pet is welcome in, and a driver says whether
    -- theirs is one. Neither is, until someone says so.
    pets SMALLINT NOT NULL
);
CREATE INDEX wl_ride_rider ON wl_ride (rider, requested_at);
CREATE INDEX wl_ride_driver ON wl_ride (driver, requested_at);
CREATE INDEX wl_ride_state ON wl_ride (state);

-- Every change of a ride's state, for the admin's view of what happened.
CREATE TABLE wl_ride_event (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    ride_id VARCHAR(32) NOT NULL,
    state VARCHAR(24) NOT NULL,
    actor VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX wl_ride_event_ride ON wl_ride_event (ride_id, created_at);

-- The drivers who passed on a ride, or let its offer lapse: they are not asked
-- again for the same ride.
CREATE TABLE wl_ride_passed (
    ride_id VARCHAR(32) NOT NULL,
    driver VARCHAR(190) NOT NULL,
    PRIMARY KEY (ride_id, driver)
);

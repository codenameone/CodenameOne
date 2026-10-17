CREATE TABLE delivery (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    state VARCHAR(24) NOT NULL,
    customer VARCHAR(190) NOT NULL,
    -- The courier it is offered to, then the courier who took it. Empty until one
    -- is found, and again between one courier passing and the next being asked.
    courier VARCHAR(190) NOT NULL,
    pickup_lat DOUBLE PRECISION NOT NULL,
    pickup_lng DOUBLE PRECISION NOT NULL,
    pickup_address VARCHAR(255) NOT NULL,
    dropoff_lat DOUBLE PRECISION NOT NULL,
    dropoff_lng DOUBLE PRECISION NOT NULL,
    dropoff_address VARCHAR(255) NOT NULL,
    fare_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    distance_meters DOUBLE PRECISION NOT NULL,
    duration_seconds DOUBLE PRECISION NOT NULL,
    requested_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    -- When the offer in hand lapses, or when the search for a courier gives up.
    offer_expires_at BIGINT NOT NULL,
    search_until BIGINT NOT NULL,
    stars SMALLINT NOT NULL,
    payment_status VARCHAR(16) NOT NULL,
    -- What the customer asked for beyond two points, how the delivery is paid for, and
    -- the parts its price is made of. Every amount is cents, fixed when the delivery is
    -- asked for; the tip and the courier's share are written when they are known.
    product VARCHAR(16) NOT NULL,
    method_id VARCHAR(32) NOT NULL,
    method_label VARCHAR(64) NOT NULL,
    base_cents BIGINT NOT NULL,
    distance_cents BIGINT NOT NULL,
    time_cents BIGINT NOT NULL,
    fee_cents BIGINT NOT NULL,
    tip_cents BIGINT NOT NULL,
    courier_cents BIGINT NOT NULL,
    women_only SMALLINT NOT NULL,
    quiet SMALLINT NOT NULL,
    wheelchair SMALLINT NOT NULL,
    note VARCHAR(255) NOT NULL,
    -- A customer may ask for a car their pet is welcome in, and a courier says whether
    -- theirs is one. Neither is, until someone says so.
    pets SMALLINT NOT NULL
);
CREATE INDEX delivery_customer ON delivery (customer, requested_at);
CREATE INDEX delivery_courier ON delivery (courier, requested_at);
CREATE INDEX delivery_state ON delivery (state);

-- Every change of a delivery's state, for the admin's view of what happened.
CREATE TABLE delivery_event (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    delivery_id VARCHAR(32) NOT NULL,
    state VARCHAR(24) NOT NULL,
    actor VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX delivery_event_delivery ON delivery_event (delivery_id, created_at);

-- The couriers who passed on a delivery, or let its offer lapse: they are not asked
-- again for the same delivery.
CREATE TABLE delivery_passed (
    delivery_id VARCHAR(32) NOT NULL,
    courier VARCHAR(190) NOT NULL,
    PRIMARY KEY (delivery_id, courier)
);

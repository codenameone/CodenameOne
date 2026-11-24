-- The SQL Server version of ../V4__deliveries.sql, which every other engine shares.
-- Text is NVARCHAR, since VARCHAR holds only the database's code page,
-- under a binary collation: the default one folds case, and a key must
-- keep "A" and "a" apart. A key column cannot change collation once
-- created, so it is declared here rather than converted later.

-- The courier's leg of an order: from the restaurant to the customer's door.
-- It has the id of the order it carries.
CREATE TABLE delivery (
    id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    state NVARCHAR(24) COLLATE Latin1_General_100_BIN2 NOT NULL,
    customer NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    -- The courier it is offered to, then the courier who took it. Empty until one
    -- is found, and again between one courier passing and the next being asked.
    courier NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
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
    -- When the offer in hand lapses, or when the search for a courier gives up.
    offer_expires_at BIGINT NOT NULL,
    search_until BIGINT NOT NULL,
    stars SMALLINT NOT NULL,
    payment_status NVARCHAR(16) COLLATE Latin1_General_100_BIN2 NOT NULL,
    -- How the order it carries is paid for. The courier is paid the order's
    -- delivery fee (`fare_cents`) and its tip, as `courier_cents` and
    -- `tip_cents`; the customer's charge is the order's, not this row's.
    method_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    method_label NVARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL,
    fee_cents BIGINT NOT NULL,
    tip_cents BIGINT NOT NULL,
    courier_cents BIGINT NOT NULL,
    -- For the courier: a gate code, which door.
    note NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL
);
CREATE INDEX delivery_customer ON delivery (customer, requested_at);
CREATE INDEX delivery_courier ON delivery (courier, requested_at);
CREATE INDEX delivery_state ON delivery (state);

-- Every change of a delivery's state, for the admin's view of what happened.
CREATE TABLE delivery_event (
    id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    delivery_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    state NVARCHAR(24) COLLATE Latin1_General_100_BIN2 NOT NULL,
    actor NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX delivery_event_delivery ON delivery_event (delivery_id, created_at);

-- The couriers who passed on a delivery, or let its offer lapse: they are not asked
-- again for the same delivery.
CREATE TABLE delivery_passed (
    delivery_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    courier NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    PRIMARY KEY (delivery_id, courier)
);

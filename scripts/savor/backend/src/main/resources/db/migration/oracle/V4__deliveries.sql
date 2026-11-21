-- The Oracle version of ../V4__deliveries.sql, which every other engine shares.
-- Oracle 19c has no BIGINT, INTEGER is NUMBER(38) and DOUBLE PRECISION
-- a decimal float, so the columns are the types the ORM declares there.
-- Text columns are nullable even when the entity requires them: Oracle
-- stores '' as NULL, so a NOT NULL column could never hold an empty
-- string; the ORM enforces 'required' itself and reads NULL back as ''.
-- A DEFAULT comes before NOT NULL, the only order Oracle accepts, and a
-- column named by an Oracle reserved word is quoted.

-- The courier's leg of an order: from the restaurant to the customer's door.
-- It has the id of the order it carries.
CREATE TABLE delivery (
    id VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    state VARCHAR2(24 CHAR),
    customer VARCHAR2(190 CHAR),
    -- The courier it is offered to, then the courier who took it. Empty until one
    -- is found, and again between one courier passing and the next being asked.
    courier VARCHAR2(190 CHAR),
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
    -- When the offer in hand lapses, or when the search for a courier gives up.
    offer_expires_at NUMBER(19) NOT NULL,
    search_until NUMBER(19) NOT NULL,
    stars NUMBER(5) NOT NULL,
    payment_status VARCHAR2(16 CHAR),
    -- How the order it carries is paid for. The courier is paid the order's
    -- delivery fee (`fare_cents`) and its tip, as `courier_cents` and
    -- `tip_cents`; the customer's charge is the order's, not this row's.
    method_id VARCHAR2(32 CHAR),
    method_label VARCHAR2(64 CHAR),
    fee_cents NUMBER(19) NOT NULL,
    tip_cents NUMBER(19) NOT NULL,
    courier_cents NUMBER(19) NOT NULL,
    -- For the courier: a gate code, which door.
    note VARCHAR2(255 CHAR)
);
CREATE INDEX delivery_customer ON delivery (customer, requested_at);
CREATE INDEX delivery_courier ON delivery (courier, requested_at);
CREATE INDEX delivery_state ON delivery (state);

-- Every change of a delivery's state, for the admin's view of what happened.
CREATE TABLE delivery_event (
    id VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    delivery_id VARCHAR2(32 CHAR),
    state VARCHAR2(24 CHAR),
    actor VARCHAR2(190 CHAR),
    created_at NUMBER(19) NOT NULL
);
CREATE INDEX delivery_event_delivery ON delivery_event (delivery_id, created_at);

-- The couriers who passed on a delivery, or let its offer lapse: they are not asked
-- again for the same delivery.
CREATE TABLE delivery_passed (
    delivery_id VARCHAR2(32 CHAR),
    courier VARCHAR2(190 CHAR),
    PRIMARY KEY (delivery_id, courier)
);

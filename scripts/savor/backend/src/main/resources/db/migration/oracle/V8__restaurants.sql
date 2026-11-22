-- The Oracle version of ../V8__restaurants.sql, which every other engine shares.
-- Oracle 19c has no BIGINT, INTEGER is NUMBER(38) and DOUBLE PRECISION
-- a decimal float, so the columns are the types the ORM declares there.
-- Text columns are nullable even when the entity requires them: Oracle
-- stores '' as NULL, so a NOT NULL column could never hold an empty
-- string; the ORM enforces 'required' itself and reads NULL back as ''.
-- A DEFAULT comes before NOT NULL, the only order Oracle accepts, and a
-- column named by an Oracle reserved word is quoted.

-- A restaurant on the service. It is listed once an admin approves it, and
-- takes orders while it is open and its staff have not paused it. Every amount
-- is cents in the service's currency.
CREATE TABLE restaurant (
    id VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    name VARCHAR2(120 CHAR),
    cuisine VARCHAR2(32 CHAR),
    description VARCHAR2(500 CHAR),
    address VARCHAR2(255 CHAR),
    lat BINARY_DOUBLE NOT NULL,
    lng BINARY_DOUBLE NOT NULL,
    -- pending, approved, rejected or suspended.
    status VARCHAR2(16 CHAR),
    -- Paused by its staff: listed, and not taking orders.
    paused NUMBER(5) NOT NULL,
    -- The hours it takes orders, as minutes after local midnight.
    opens_at NUMBER(10) NOT NULL,
    closes_at NUMBER(10) NOT NULL,
    prep_minutes NUMBER(10) NOT NULL,
    minimum_cents NUMBER(19) NOT NULL,
    -- One to four: how the menu's prices compare.
    price_level NUMBER(5) NOT NULL,
    rating_total NUMBER(19) NOT NULL,
    rating_count NUMBER(19) NOT NULL,
    photo_id VARCHAR2(32 CHAR),
    applicant VARCHAR2(190 CHAR),
    reason VARCHAR2(255 CHAR),
    created_at NUMBER(19) NOT NULL,
    updated_at NUMBER(19) NOT NULL
);
CREATE INDEX restaurant_status ON restaurant (status, lat, lng);
CREATE INDEX restaurant_place ON restaurant (lat, lng);

-- Who works for a restaurant: takes its orders and keeps its menu.
CREATE TABLE restaurant_staff (
    restaurant_id VARCHAR2(32 CHAR),
    username VARCHAR2(190 CHAR),
    PRIMARY KEY (restaurant_id, username)
);
CREATE INDEX restaurant_staff_username ON restaurant_staff (username);

-- The menu: sections in the order the restaurant lists them, dishes in each,
-- and the extras a dish can be ordered with.
CREATE TABLE menu_section (
    id VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    restaurant_id VARCHAR2(32 CHAR),
    name VARCHAR2(80 CHAR),
    position NUMBER(10) NOT NULL
);
CREATE INDEX menu_section_restaurant ON menu_section (restaurant_id, position);

CREATE TABLE dish (
    id VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    restaurant_id VARCHAR2(32 CHAR),
    section_id VARCHAR2(32 CHAR),
    name VARCHAR2(120 CHAR),
    description VARCHAR2(500 CHAR),
    price_cents NUMBER(19) NOT NULL,
    photo_id VARCHAR2(32 CHAR),
    -- Sold out for now: listed, and not orderable.
    available NUMBER(5) NOT NULL,
    popular NUMBER(5) NOT NULL,
    position NUMBER(10) NOT NULL
);
CREATE INDEX dish_restaurant ON dish (restaurant_id, section_id, position);

CREATE TABLE dish_option (
    id VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    dish_id VARCHAR2(32 CHAR),
    name VARCHAR2(80 CHAR),
    price_cents NUMBER(19) NOT NULL,
    position NUMBER(10) NOT NULL
);
CREATE INDEX dish_option_dish ON dish_option (dish_id, position);

-- An order. Its lines, prices and address are copied in when it is placed, so a
-- later change of menu or of prices does not rewrite what was bought. Its
-- delivery, once the restaurant accepts it, has the same id.
CREATE TABLE customer_order (
    id VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    customer VARCHAR2(190 CHAR),
    restaurant_id VARCHAR2(32 CHAR),
    restaurant_name VARCHAR2(120 CHAR),
    state VARCHAR2(24 CHAR),
    subtotal_cents NUMBER(19) NOT NULL,
    delivery_fee_cents NUMBER(19) NOT NULL,
    service_fee_cents NUMBER(19) NOT NULL,
    small_order_cents NUMBER(19) NOT NULL,
    tax_cents NUMBER(19) NOT NULL,
    discount_cents NUMBER(19) NOT NULL,
    tip_cents NUMBER(19) NOT NULL,
    total_cents NUMBER(19) NOT NULL,
    commission_cents NUMBER(19) NOT NULL,
    currency VARCHAR2(3 CHAR),
    promo_code VARCHAR2(32 CHAR),
    dropoff_lat BINARY_DOUBLE NOT NULL,
    dropoff_lng BINARY_DOUBLE NOT NULL,
    dropoff_address VARCHAR2(255 CHAR),
    note VARCHAR2(255 CHAR),
    method_id VARCHAR2(32 CHAR),
    method_label VARCHAR2(64 CHAR),
    -- The processor's hold on the card, taken when the order is placed and
    -- captured when it is delivered. Empty for cash.
    authorization_ref VARCHAR2(64 CHAR),
    -- AUTHORIZED, then PAID, RELEASED, FAILED or REFUNDED.
    payment_status VARCHAR2(16 CHAR),
    prep_minutes NUMBER(10) NOT NULL,
    placed_at NUMBER(19) NOT NULL,
    accepted_at NUMBER(19) NOT NULL,
    ready_at NUMBER(19) NOT NULL,
    delivered_at NUMBER(19) NOT NULL,
    updated_at NUMBER(19) NOT NULL,
    reason VARCHAR2(255 CHAR),
    stars NUMBER(5) NOT NULL
);
CREATE INDEX customer_order_customer ON customer_order (customer, placed_at);
CREATE INDEX customer_order_restaurant ON customer_order (restaurant_id, state, placed_at);
CREATE INDEX customer_order_state ON customer_order (state, updated_at);

CREATE TABLE order_line (
    id VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    order_id VARCHAR2(32 CHAR),
    dish_id VARCHAR2(32 CHAR),
    name VARCHAR2(120 CHAR),
    options VARCHAR2(500 CHAR),
    quantity NUMBER(10) NOT NULL,
    unit_cents NUMBER(19) NOT NULL,
    line_cents NUMBER(19) NOT NULL,
    position NUMBER(10) NOT NULL
);
CREATE INDEX order_line_order ON order_line (order_id, position);

-- Every change of an order's state, for the admin's view of what happened.
CREATE TABLE order_event (
    id VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    order_id VARCHAR2(32 CHAR),
    state VARCHAR2(24 CHAR),
    actor VARCHAR2(190 CHAR),
    created_at NUMBER(19) NOT NULL
);
CREATE INDEX order_event_order ON order_event (order_id, created_at);

CREATE TABLE favorite (
    username VARCHAR2(190 CHAR),
    restaurant_id VARCHAR2(32 CHAR),
    created_at NUMBER(19) NOT NULL,
    PRIMARY KEY (username, restaurant_id)
);

-- A code a customer types at checkout: a percentage or an amount off the food,
-- for an order of at least `minimum_cents`, once per account.
CREATE TABLE promo (
    code VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    percent_off NUMBER(10) NOT NULL,
    amount_off_cents NUMBER(19) NOT NULL,
    minimum_cents NUMBER(19) NOT NULL,
    active NUMBER(5) NOT NULL,
    created_at NUMBER(19) NOT NULL
);

CREATE TABLE promo_use (
    code VARCHAR2(32 CHAR),
    username VARCHAR2(190 CHAR),
    order_id VARCHAR2(32 CHAR),
    PRIMARY KEY (code, username)
);

-- A customer's saved places to be delivered to.
CREATE TABLE saved_address (
    id VARCHAR2(32 CHAR) NOT NULL PRIMARY KEY,
    username VARCHAR2(190 CHAR),
    label VARCHAR2(40 CHAR),
    address VARCHAR2(255 CHAR),
    lat BINARY_DOUBLE NOT NULL,
    lng BINARY_DOUBLE NOT NULL,
    note VARCHAR2(255 CHAR),
    created_at NUMBER(19) NOT NULL
);
CREATE INDEX saved_address_username ON saved_address (username, created_at);

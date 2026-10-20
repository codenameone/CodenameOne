-- A restaurant on the service. It is listed once an admin approves it, and
-- takes orders while it is open and its staff have not paused it. Every amount
-- is cents in the service's currency.
CREATE TABLE restaurant (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    cuisine VARCHAR(32) NOT NULL,
    description VARCHAR(500) NOT NULL,
    address VARCHAR(255) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    -- pending, approved, rejected or suspended.
    status VARCHAR(16) NOT NULL,
    -- Paused by its staff: listed, and not taking orders.
    paused SMALLINT NOT NULL,
    -- The hours it takes orders, as minutes after local midnight.
    opens_at INTEGER NOT NULL,
    closes_at INTEGER NOT NULL,
    prep_minutes INTEGER NOT NULL,
    minimum_cents BIGINT NOT NULL,
    -- One to four: how the menu's prices compare.
    price_level SMALLINT NOT NULL,
    rating_total BIGINT NOT NULL,
    rating_count BIGINT NOT NULL,
    photo_id VARCHAR(32) NOT NULL,
    applicant VARCHAR(190) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX restaurant_status ON restaurant (status, lat, lng);
CREATE INDEX restaurant_place ON restaurant (lat, lng);

-- Who works for a restaurant: takes its orders and keeps its menu.
CREATE TABLE restaurant_staff (
    restaurant_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    PRIMARY KEY (restaurant_id, username)
);
CREATE INDEX restaurant_staff_username ON restaurant_staff (username);

-- The menu: sections in the order the restaurant lists them, dishes in each,
-- and the extras a dish can be ordered with.
CREATE TABLE menu_section (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    restaurant_id VARCHAR(32) NOT NULL,
    name VARCHAR(80) NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX menu_section_restaurant ON menu_section (restaurant_id, position);

CREATE TABLE dish (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    restaurant_id VARCHAR(32) NOT NULL,
    section_id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NOT NULL,
    price_cents BIGINT NOT NULL,
    photo_id VARCHAR(32) NOT NULL,
    -- Sold out for now: listed, and not orderable.
    available SMALLINT NOT NULL,
    popular SMALLINT NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX dish_restaurant ON dish (restaurant_id, section_id, position);

CREATE TABLE dish_option (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    dish_id VARCHAR(32) NOT NULL,
    name VARCHAR(80) NOT NULL,
    price_cents BIGINT NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX dish_option_dish ON dish_option (dish_id, position);

-- An order. Its lines, prices and address are copied in when it is placed, so a
-- later change of menu or of prices does not rewrite what was bought. Its
-- delivery, once the restaurant accepts it, has the same id.
CREATE TABLE customer_order (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    customer VARCHAR(190) NOT NULL,
    restaurant_id VARCHAR(32) NOT NULL,
    restaurant_name VARCHAR(120) NOT NULL,
    state VARCHAR(24) NOT NULL,
    subtotal_cents BIGINT NOT NULL,
    delivery_fee_cents BIGINT NOT NULL,
    service_fee_cents BIGINT NOT NULL,
    small_order_cents BIGINT NOT NULL,
    tax_cents BIGINT NOT NULL,
    discount_cents BIGINT NOT NULL,
    tip_cents BIGINT NOT NULL,
    total_cents BIGINT NOT NULL,
    commission_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    promo_code VARCHAR(32) NOT NULL,
    dropoff_lat DOUBLE PRECISION NOT NULL,
    dropoff_lng DOUBLE PRECISION NOT NULL,
    dropoff_address VARCHAR(255) NOT NULL,
    note VARCHAR(255) NOT NULL,
    method_id VARCHAR(32) NOT NULL,
    method_label VARCHAR(64) NOT NULL,
    -- The processor's hold on the card, taken when the order is placed and
    -- captured when it is delivered. Empty for cash.
    authorization_ref VARCHAR(64) NOT NULL,
    -- AUTHORIZED, then PAID, RELEASED, FAILED or REFUNDED.
    payment_status VARCHAR(16) NOT NULL,
    prep_minutes INTEGER NOT NULL,
    placed_at BIGINT NOT NULL,
    accepted_at BIGINT NOT NULL,
    ready_at BIGINT NOT NULL,
    delivered_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    stars SMALLINT NOT NULL
);
CREATE INDEX customer_order_customer ON customer_order (customer, placed_at);
CREATE INDEX customer_order_restaurant ON customer_order (restaurant_id, state, placed_at);
CREATE INDEX customer_order_state ON customer_order (state, updated_at);

CREATE TABLE order_line (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    order_id VARCHAR(32) NOT NULL,
    dish_id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    options VARCHAR(500) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_cents BIGINT NOT NULL,
    line_cents BIGINT NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX order_line_order ON order_line (order_id, position);

-- Every change of an order's state, for the admin's view of what happened.
CREATE TABLE order_event (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    order_id VARCHAR(32) NOT NULL,
    state VARCHAR(24) NOT NULL,
    actor VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX order_event_order ON order_event (order_id, created_at);

CREATE TABLE favorite (
    username VARCHAR(190) NOT NULL,
    restaurant_id VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (username, restaurant_id)
);

-- A code a customer types at checkout: a percentage or an amount off the food,
-- for an order of at least `minimum_cents`, once per account.
CREATE TABLE promo (
    code VARCHAR(32) NOT NULL PRIMARY KEY,
    percent_off INTEGER NOT NULL,
    amount_off_cents BIGINT NOT NULL,
    minimum_cents BIGINT NOT NULL,
    active SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE promo_use (
    code VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    PRIMARY KEY (code, username)
);

-- A customer's saved places to be delivered to.
CREATE TABLE saved_address (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    label VARCHAR(40) NOT NULL,
    address VARCHAR(255) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    note VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX saved_address_username ON saved_address (username, created_at);

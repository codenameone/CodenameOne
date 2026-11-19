-- The SQL Server version of ../V8__restaurants.sql, which every other engine shares.
-- Text is NVARCHAR, since VARCHAR holds only the database's code page,
-- under a binary collation: the default one folds case, and a key must
-- keep "A" and "a" apart. A key column cannot change collation once
-- created, so it is declared here rather than converted later.

-- A restaurant on the service. It is listed once an admin approves it, and
-- takes orders while it is open and its staff have not paused it. Every amount
-- is cents in the service's currency.
CREATE TABLE restaurant (
    id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    name NVARCHAR(120) COLLATE Latin1_General_100_BIN2 NOT NULL,
    cuisine NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    description NVARCHAR(500) COLLATE Latin1_General_100_BIN2 NOT NULL,
    address NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    -- pending, approved, rejected or suspended.
    status NVARCHAR(16) COLLATE Latin1_General_100_BIN2 NOT NULL,
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
    photo_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    applicant NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    reason NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX restaurant_status ON restaurant (status, lat, lng);
CREATE INDEX restaurant_place ON restaurant (lat, lng);

-- Who works for a restaurant: takes its orders and keeps its menu.
CREATE TABLE restaurant_staff (
    restaurant_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    username NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    PRIMARY KEY (restaurant_id, username)
);
CREATE INDEX restaurant_staff_username ON restaurant_staff (username);

-- The menu: sections in the order the restaurant lists them, dishes in each,
-- and the extras a dish can be ordered with.
CREATE TABLE menu_section (
    id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    restaurant_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    name NVARCHAR(80) COLLATE Latin1_General_100_BIN2 NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX menu_section_restaurant ON menu_section (restaurant_id, position);

CREATE TABLE dish (
    id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    restaurant_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    section_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    name NVARCHAR(120) COLLATE Latin1_General_100_BIN2 NOT NULL,
    description NVARCHAR(500) COLLATE Latin1_General_100_BIN2 NOT NULL,
    price_cents BIGINT NOT NULL,
    photo_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    -- Sold out for now: listed, and not orderable.
    available SMALLINT NOT NULL,
    popular SMALLINT NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX dish_restaurant ON dish (restaurant_id, section_id, position);

CREATE TABLE dish_option (
    id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    dish_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    name NVARCHAR(80) COLLATE Latin1_General_100_BIN2 NOT NULL,
    price_cents BIGINT NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX dish_option_dish ON dish_option (dish_id, position);

-- An order. Its lines, prices and address are copied in when it is placed, so a
-- later change of menu or of prices does not rewrite what was bought. Its
-- delivery, once the restaurant accepts it, has the same id.
CREATE TABLE customer_order (
    id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    customer NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    restaurant_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    restaurant_name NVARCHAR(120) COLLATE Latin1_General_100_BIN2 NOT NULL,
    state NVARCHAR(24) COLLATE Latin1_General_100_BIN2 NOT NULL,
    subtotal_cents BIGINT NOT NULL,
    delivery_fee_cents BIGINT NOT NULL,
    service_fee_cents BIGINT NOT NULL,
    small_order_cents BIGINT NOT NULL,
    tax_cents BIGINT NOT NULL,
    discount_cents BIGINT NOT NULL,
    tip_cents BIGINT NOT NULL,
    total_cents BIGINT NOT NULL,
    commission_cents BIGINT NOT NULL,
    currency NVARCHAR(3) COLLATE Latin1_General_100_BIN2 NOT NULL,
    promo_code NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    dropoff_lat DOUBLE PRECISION NOT NULL,
    dropoff_lng DOUBLE PRECISION NOT NULL,
    dropoff_address NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL,
    note NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL,
    method_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    method_label NVARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL,
    -- The processor's hold on the card, taken when the order is placed and
    -- captured when it is delivered. Empty for cash.
    authorization_ref NVARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL,
    -- AUTHORIZED, then PAID, RELEASED, FAILED or REFUNDED.
    payment_status NVARCHAR(16) COLLATE Latin1_General_100_BIN2 NOT NULL,
    prep_minutes INTEGER NOT NULL,
    placed_at BIGINT NOT NULL,
    accepted_at BIGINT NOT NULL,
    ready_at BIGINT NOT NULL,
    delivered_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    reason NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL,
    stars SMALLINT NOT NULL
);
CREATE INDEX customer_order_customer ON customer_order (customer, placed_at);
CREATE INDEX customer_order_restaurant ON customer_order (restaurant_id, state, placed_at);
CREATE INDEX customer_order_state ON customer_order (state, updated_at);

CREATE TABLE order_line (
    id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    order_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    dish_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    name NVARCHAR(120) COLLATE Latin1_General_100_BIN2 NOT NULL,
    options NVARCHAR(500) COLLATE Latin1_General_100_BIN2 NOT NULL,
    quantity INTEGER NOT NULL,
    unit_cents BIGINT NOT NULL,
    line_cents BIGINT NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX order_line_order ON order_line (order_id, position);

-- Every change of an order's state, for the admin's view of what happened.
CREATE TABLE order_event (
    id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    order_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    state NVARCHAR(24) COLLATE Latin1_General_100_BIN2 NOT NULL,
    actor NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX order_event_order ON order_event (order_id, created_at);

CREATE TABLE favorite (
    username NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    restaurant_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (username, restaurant_id)
);

-- A code a customer types at checkout: a percentage or an amount off the food,
-- for an order of at least `minimum_cents`, once per account.
CREATE TABLE promo (
    code NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    percent_off INTEGER NOT NULL,
    amount_off_cents BIGINT NOT NULL,
    minimum_cents BIGINT NOT NULL,
    active SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE promo_use (
    code NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    username NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    order_id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    PRIMARY KEY (code, username)
);

-- A customer's saved places to be delivered to.
CREATE TABLE saved_address (
    id NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    username NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    label NVARCHAR(40) COLLATE Latin1_General_100_BIN2 NOT NULL,
    address NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    note NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX saved_address_username ON saved_address (username, created_at);

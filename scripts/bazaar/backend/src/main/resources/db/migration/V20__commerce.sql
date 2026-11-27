-- The commerce schema: stores, catalog, inventory, orders, fulfilment, the
-- point of sale and the rest. Generated from the entity classes, which are the
-- source of truth; amounts are cents, times are epoch milliseconds, flags 0/1.

-- A shop on the platform: its storefront address, currency and look.
CREATE TABLE store (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    handle VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    tagline VARCHAR(200) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    accent_color INTEGER NOT NULL,
    logo_photo_id VARCHAR(32) NOT NULL,
    hero_photo_id VARCHAR(32) NOT NULL,
    owner_username VARCHAR(190) NOT NULL,
    status VARCHAR(16) NOT NULL,
    tax_inclusive SMALLINT NOT NULL,
    email VARCHAR(190) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    address VARCHAR(255) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    policies TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX store_status ON store (status);
CREATE UNIQUE INDEX store_handle_key ON store (handle);

-- Who works for a store, with what role, and the PIN they use at a register.
CREATE TABLE store_staff (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    role VARCHAR(16) NOT NULL,
    pin_hash VARCHAR(128) NOT NULL,
    location_id VARCHAR(32) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX store_staff_username ON store_staff (username);
CREATE UNIQUE INDEX store_staff_store_id_username_key ON store_staff (store_id, username);

-- A place stock is kept: a warehouse, a shop, a pop-up stall.
CREATE TABLE location (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    address VARCHAR(255) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    fulfils_online SMALLINT NOT NULL,
    active SMALLINT NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX location_store_id_position ON location (store_id, position);

-- Something a store sells. What a buyer picks is one of its variants.
CREATE TABLE product (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    handle VARCHAR(120) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    vendor VARCHAR(120) NOT NULL,
    product_type VARCHAR(80) NOT NULL,
    tags VARCHAR(500) NOT NULL,
    status VARCHAR(16) NOT NULL,
    seo_title VARCHAR(200) NOT NULL,
    seo_description VARCHAR(320) NOT NULL,
    photo_id VARCHAR(32) NOT NULL,
    rating_total BIGINT NOT NULL,
    rating_count BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX product_store_id_status ON product (store_id, status);
CREATE UNIQUE INDEX product_store_id_handle_key ON product (store_id, handle);

-- A picture in a product's gallery, in display order.
CREATE TABLE product_photo (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    product_id VARCHAR(32) NOT NULL,
    photo_id VARCHAR(32) NOT NULL,
    alt VARCHAR(200) NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX product_photo_product_id_position ON product_photo (product_id, position);

-- A dimension a product varies by, such as Size, and its values.
CREATE TABLE product_option (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    product_id VARCHAR(32) NOT NULL,
    name VARCHAR(60) NOT NULL,
    option_values VARCHAR(500) NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX product_option_product_id_position ON product_option (product_id, position);

-- One buyable combination of a product's options, with its own SKU, barcode and price.
CREATE TABLE variant (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    product_id VARCHAR(32) NOT NULL,
    store_id VARCHAR(32) NOT NULL,
    title VARCHAR(200) NOT NULL,
    option1 VARCHAR(80) NOT NULL,
    option2 VARCHAR(80) NOT NULL,
    option3 VARCHAR(80) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    barcode VARCHAR(64) NOT NULL,
    price_cents BIGINT NOT NULL,
    compare_at_cents BIGINT NOT NULL,
    cost_cents BIGINT NOT NULL,
    weight_grams INTEGER NOT NULL,
    taxable SMALLINT NOT NULL,
    track_inventory SMALLINT NOT NULL,
    allow_backorder SMALLINT NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX variant_product_id_position ON variant (product_id, position);
CREATE INDEX variant_store_id_sku ON variant (store_id, sku);
CREATE INDEX variant_store_id_barcode ON variant (store_id, barcode);

-- A group of products shown together. Manual when `rule` is empty, otherwise filled by the rule.
CREATE TABLE collection (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    handle VARCHAR(120) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    photo_id VARCHAR(32) NOT NULL,
    rule_text VARCHAR(500) NOT NULL,
    position INTEGER NOT NULL,
    published SMALLINT NOT NULL
);
CREATE INDEX collection_store_id_position ON collection (store_id, position);
CREATE UNIQUE INDEX collection_store_id_handle_key ON collection (store_id, handle);

-- A product placed by hand in a manual collection.
CREATE TABLE collection_product (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    collection_id VARCHAR(32) NOT NULL,
    product_id VARCHAR(32) NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX collection_product_collection_id_position ON collection_product (collection_id, position);
CREATE INDEX collection_product_product_id ON collection_product (product_id);

-- Stock of one variant at one location. Available is `onHand - committed`; every change is an InventoryMove.
CREATE TABLE inventory_level (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    location_id VARCHAR(32) NOT NULL,
    on_hand INTEGER NOT NULL,
    committed INTEGER NOT NULL,
    incoming INTEGER NOT NULL,
    reorder_point INTEGER NOT NULL,
    version BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX inventory_level_store_id ON inventory_level (store_id);
CREATE INDEX inventory_level_location_id ON inventory_level (location_id);
CREATE UNIQUE INDEX inventory_level_variant_id_location_id_key ON inventory_level (variant_id, location_id);

-- One change to a stock level and why: the ledger the levels are kept from.
CREATE TABLE inventory_move (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    location_id VARCHAR(32) NOT NULL,
    on_hand_delta INTEGER NOT NULL,
    committed_delta INTEGER NOT NULL,
    reason VARCHAR(16) NOT NULL,
    reference VARCHAR(64) NOT NULL,
    username VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX inventory_move_variant_id_created_at ON inventory_move (variant_id, created_at);
CREATE INDEX inventory_move_store_id_created_at ON inventory_move (store_id, created_at);

-- Stock moving between two of a store's locations.
CREATE TABLE stock_transfer (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    from_location_id VARCHAR(32) NOT NULL,
    to_location_id VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    note VARCHAR(500) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX stock_transfer_store_id_created_at ON stock_transfer (store_id, created_at);

-- A variant and quantity on a stock transfer.
CREATE TABLE transfer_line (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    transfer_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    quantity INTEGER NOT NULL,
    received INTEGER NOT NULL
);
CREATE INDEX transfer_line_transfer_id ON transfer_line (transfer_id);

-- Who a store buys stock from.
CREATE TABLE supplier (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    contact VARCHAR(120) NOT NULL,
    email VARCHAR(190) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    address VARCHAR(255) NOT NULL,
    note VARCHAR(500) NOT NULL
);
CREATE INDEX supplier_store_id ON supplier (store_id);

-- Stock ordered from a supplier, received into a location.
CREATE TABLE purchase_order (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    number INTEGER NOT NULL,
    supplier_id VARCHAR(32) NOT NULL,
    location_id VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    expected_at BIGINT NOT NULL,
    note VARCHAR(500) NOT NULL,
    total_cents BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX purchase_order_store_id_created_at ON purchase_order (store_id, created_at);

-- A variant ordered on a purchase order and how much of it arrived.
CREATE TABLE purchase_line (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    purchase_order_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    quantity INTEGER NOT NULL,
    received INTEGER NOT NULL,
    cost_cents BIGINT NOT NULL
);
CREATE INDEX purchase_line_purchase_order_id ON purchase_line (purchase_order_id);

-- A buyer known to a store, online or at the counter.
CREATE TABLE customer (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    email VARCHAR(190) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    tags VARCHAR(500) NOT NULL,
    accepts_marketing SMALLINT NOT NULL,
    note VARCHAR(1000) NOT NULL,
    orders_count INTEGER NOT NULL,
    spent_cents BIGINT NOT NULL,
    store_credit_cents BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX customer_store_id_email ON customer (store_id, email);
CREATE INDEX customer_store_id_phone ON customer (store_id, phone);
CREATE INDEX customer_username ON customer (username);

-- A place a customer has things shipped to.
CREATE TABLE customer_address (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    customer_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    name VARCHAR(120) NOT NULL,
    line1 VARCHAR(200) NOT NULL,
    line2 VARCHAR(200) NOT NULL,
    city VARCHAR(120) NOT NULL,
    region VARCHAR(80) NOT NULL,
    postal VARCHAR(20) NOT NULL,
    country VARCHAR(2) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    is_default SMALLINT NOT NULL
);
CREATE INDEX customer_address_customer_id ON customer_address (customer_id);
CREATE INDEX customer_address_username ON customer_address (username);

-- A shopper's cart in one store, kept on the server so it follows them between devices.
CREATE TABLE cart (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    token VARCHAR(64) NOT NULL,
    email VARCHAR(190) NOT NULL,
    discount_code VARCHAR(64) NOT NULL,
    gift_card_code VARCHAR(64) NOT NULL,
    reminder_sent_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX cart_username ON cart (username);
CREATE INDEX cart_updated_at ON cart (updated_at);
CREATE UNIQUE INDEX cart_token_key ON cart (token);

-- A variant and quantity in a cart.
CREATE TABLE cart_line (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    cart_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    quantity INTEGER NOT NULL,
    added_at BIGINT NOT NULL
);
CREATE INDEX cart_line_cart_id ON cart_line (cart_id);

-- An order, from the online store, a register or a draft a merchant wrote. Amounts are cents in the store's currency.
CREATE TABLE store_order (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    number INTEGER NOT NULL,
    customer_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    email VARCHAR(190) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    channel VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    financial VARCHAR(24) NOT NULL,
    fulfilment VARCHAR(16) NOT NULL,
    subtotal_cents BIGINT NOT NULL,
    discount_cents BIGINT NOT NULL,
    shipping_cents BIGINT NOT NULL,
    tax_cents BIGINT NOT NULL,
    total_cents BIGINT NOT NULL,
    refunded_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    discount_code VARCHAR(64) NOT NULL,
    ship_name VARCHAR(120) NOT NULL,
    ship_line1 VARCHAR(200) NOT NULL,
    ship_line2 VARCHAR(200) NOT NULL,
    ship_city VARCHAR(120) NOT NULL,
    ship_region VARCHAR(80) NOT NULL,
    ship_postal VARCHAR(20) NOT NULL,
    ship_country VARCHAR(2) NOT NULL,
    ship_phone VARCHAR(32) NOT NULL,
    ship_lat DOUBLE PRECISION NOT NULL,
    ship_lng DOUBLE PRECISION NOT NULL,
    shipping_rate_id VARCHAR(32) NOT NULL,
    shipping_title VARCHAR(120) NOT NULL,
    location_id VARCHAR(32) NOT NULL,
    register_id VARCHAR(32) NOT NULL,
    shift_id VARCHAR(32) NOT NULL,
    staff_username VARCHAR(190) NOT NULL,
    client_id VARCHAR(64) NOT NULL,
    note VARCHAR(1000) NOT NULL,
    tags VARCHAR(500) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    closed_at BIGINT NOT NULL
);
CREATE INDEX store_order_store_id_created_at ON store_order (store_id, created_at);
CREATE INDEX store_order_username_created_at ON store_order (username, created_at);
CREATE INDEX store_order_customer_id ON store_order (customer_id);
CREATE UNIQUE INDEX store_order_store_id_number_key ON store_order (store_id, number);
CREATE UNIQUE INDEX store_order_client_id_key ON store_order (client_id);

-- One variant on an order, at the price it was sold for.
CREATE TABLE order_line (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    order_id VARCHAR(32) NOT NULL,
    product_id VARCHAR(32) NOT NULL,
    variant_id VARCHAR(32) NOT NULL,
    title VARCHAR(200) NOT NULL,
    variant_title VARCHAR(200) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    photo_id VARCHAR(32) NOT NULL,
    quantity INTEGER NOT NULL,
    fulfilled_qty INTEGER NOT NULL,
    refunded_qty INTEGER NOT NULL,
    price_cents BIGINT NOT NULL,
    discount_cents BIGINT NOT NULL,
    tax_cents BIGINT NOT NULL,
    weight_grams INTEGER NOT NULL,
    location_id VARCHAR(32) NOT NULL
);
CREATE INDEX order_line_order_id ON order_line (order_id);
CREATE INDEX order_line_variant_id ON order_line (variant_id);

-- A line in an order's timeline.
CREATE TABLE order_event (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    order_id VARCHAR(32) NOT NULL,
    kind VARCHAR(32) NOT NULL,
    message VARCHAR(500) NOT NULL,
    username VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX order_event_order_id_created_at ON order_event (order_id, created_at);

-- A price reduction, entered as a code or applied automatically.
CREATE TABLE discount (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    code VARCHAR(64) NOT NULL,
    title VARCHAR(120) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    amount BIGINT NOT NULL,
    min_subtotal_cents BIGINT NOT NULL,
    buy_qty INTEGER NOT NULL,
    get_qty INTEGER NOT NULL,
    applies_to VARCHAR(16) NOT NULL,
    target_id VARCHAR(32) NOT NULL,
    automatic SMALLINT NOT NULL,
    usage_limit INTEGER NOT NULL,
    used_count INTEGER NOT NULL,
    once_per_customer SMALLINT NOT NULL,
    combinable SMALLINT NOT NULL,
    starts_at BIGINT NOT NULL,
    ends_at BIGINT NOT NULL,
    active SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX discount_store_id_code ON discount (store_id, code);

-- A discount used on an order, for usage limits.
CREATE TABLE discount_use (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    discount_id VARCHAR(32) NOT NULL,
    customer_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX discount_use_discount_id ON discount_use (discount_id);

-- A stored-value card, sold or issued as store credit.
CREATE TABLE gift_card (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    code VARCHAR(32) NOT NULL,
    last_four VARCHAR(4) NOT NULL,
    initial_cents BIGINT NOT NULL,
    balance_cents BIGINT NOT NULL,
    customer_id VARCHAR(32) NOT NULL,
    note VARCHAR(200) NOT NULL,
    expires_at BIGINT NOT NULL,
    active SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX gift_card_store_id ON gift_card (store_id);
CREATE UNIQUE INDEX gift_card_code_key ON gift_card (code);

-- Money taken from or added to a gift card.
CREATE TABLE gift_card_tx (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    gift_card_id VARCHAR(32) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    delta_cents BIGINT NOT NULL,
    note VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX gift_card_tx_gift_card_id ON gift_card_tx (gift_card_id);

-- The tax charged where an order goes. An empty region matches the whole country.
CREATE TABLE tax_rate (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    country VARCHAR(2) NOT NULL,
    region VARCHAR(80) NOT NULL,
    name VARCHAR(80) NOT NULL,
    basis_points INTEGER NOT NULL,
    applies_to_shipping SMALLINT NOT NULL
);
CREATE INDEX tax_rate_store_id_country ON tax_rate (store_id, country);

-- A set of countries that share shipping rates.
CREATE TABLE shipping_zone (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    countries VARCHAR(500) NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX shipping_zone_store_id ON shipping_zone (store_id);

-- A way to get an order to a zone and what it costs.
CREATE TABLE shipping_rate (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    zone_id VARCHAR(32) NOT NULL,
    store_id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    price_cents BIGINT NOT NULL,
    min_value BIGINT NOT NULL,
    max_value BIGINT NOT NULL,
    carrier VARCHAR(32) NOT NULL,
    service VARCHAR(64) NOT NULL,
    eta_days_min INTEGER NOT NULL,
    eta_days_max INTEGER NOT NULL,
    location_id VARCHAR(32) NOT NULL,
    radius_meters INTEGER NOT NULL,
    position INTEGER NOT NULL
);
CREATE INDEX shipping_rate_zone_id_position ON shipping_rate (zone_id, position);
CREATE INDEX shipping_rate_store_id ON shipping_rate (store_id);

-- Part or all of an order leaving one location: packed, labelled, tracked.
CREATE TABLE fulfillment (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    order_id VARCHAR(32) NOT NULL,
    store_id VARCHAR(32) NOT NULL,
    location_id VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL,
    carrier VARCHAR(32) NOT NULL,
    service VARCHAR(64) NOT NULL,
    tracking_number VARCHAR(64) NOT NULL,
    label_cents BIGINT NOT NULL,
    weight_grams INTEGER NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX fulfillment_order_id ON fulfillment (order_id);
CREATE INDEX fulfillment_store_id_status ON fulfillment (store_id, status);

-- An order line and quantity in a fulfillment.
CREATE TABLE fulfillment_line (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    fulfillment_id VARCHAR(32) NOT NULL,
    order_line_id VARCHAR(32) NOT NULL,
    quantity INTEGER NOT NULL
);
CREATE INDEX fulfillment_line_fulfillment_id ON fulfillment_line (fulfillment_id);

-- A carrier scan: where a parcel was and what happened to it.
CREATE TABLE tracking_event (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    fulfillment_id VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL,
    message VARCHAR(200) NOT NULL,
    place VARCHAR(120) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX tracking_event_fulfillment_id_created_at ON tracking_event (fulfillment_id, created_at);

-- Money returned on an order, optionally putting the stock back.
CREATE TABLE refund (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    order_id VARCHAR(32) NOT NULL,
    amount_cents BIGINT NOT NULL,
    shipping_cents BIGINT NOT NULL,
    reason VARCHAR(200) NOT NULL,
    restock SMALLINT NOT NULL,
    username VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX refund_order_id ON refund (order_id);

-- An order line and quantity refunded.
CREATE TABLE refund_line (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    refund_id VARCHAR(32) NOT NULL,
    order_line_id VARCHAR(32) NOT NULL,
    quantity INTEGER NOT NULL
);
CREATE INDEX refund_line_refund_id ON refund_line (refund_id);

-- A buyer asking to send something back.
CREATE TABLE return_request (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    order_id VARCHAR(32) NOT NULL,
    store_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    status VARCHAR(16) NOT NULL,
    reason VARCHAR(200) NOT NULL,
    note VARCHAR(1000) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX return_request_order_id ON return_request (order_id);
CREATE INDEX return_request_store_id_status ON return_request (store_id, status);

-- An order line and quantity being returned.
CREATE TABLE return_line (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    return_id VARCHAR(32) NOT NULL,
    order_line_id VARCHAR(32) NOT NULL,
    quantity INTEGER NOT NULL
);
CREATE INDEX return_line_return_id ON return_line (return_id);

-- A point-of-sale terminal at a location.
CREATE TABLE register (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    location_id VARCHAR(32) NOT NULL,
    name VARCHAR(80) NOT NULL,
    active SMALLINT NOT NULL
);
CREATE INDEX register_store_id ON register (store_id);

-- A cashier's session at a register, from opening float to cash count.
CREATE TABLE shift (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    register_id VARCHAR(32) NOT NULL,
    store_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    status VARCHAR(16) NOT NULL,
    opening_cents BIGINT NOT NULL,
    cash_sales_cents BIGINT NOT NULL,
    card_sales_cents BIGINT NOT NULL,
    other_sales_cents BIGINT NOT NULL,
    cash_moves_cents BIGINT NOT NULL,
    expected_cents BIGINT NOT NULL,
    counted_cents BIGINT NOT NULL,
    sales_count INTEGER NOT NULL,
    note VARCHAR(500) NOT NULL,
    opened_at BIGINT NOT NULL,
    closed_at BIGINT NOT NULL
);
CREATE INDEX shift_register_id_opened_at ON shift (register_id, opened_at);
CREATE INDEX shift_store_id_opened_at ON shift (store_id, opened_at);

-- Cash put into or taken out of a drawer during a shift.
CREATE TABLE cash_move (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    shift_id VARCHAR(32) NOT NULL,
    delta_cents BIGINT NOT NULL,
    reason VARCHAR(200) NOT NULL,
    username VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX cash_move_shift_id ON cash_move (shift_id);

-- How part of an order was paid: card, cash, gift card or store credit.
CREATE TABLE tender (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    order_id VARCHAR(32) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    amount_cents BIGINT NOT NULL,
    change_cents BIGINT NOT NULL,
    reference VARCHAR(64) NOT NULL,
    payment_id VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX tender_order_id ON tender (order_id);

-- A buyer's rating of a product. Published once the store approves it.
CREATE TABLE review (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    product_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    author_name VARCHAR(80) NOT NULL,
    rating INTEGER NOT NULL,
    title VARCHAR(120) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    status VARCHAR(16) NOT NULL,
    verified SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX review_product_id_status ON review (product_id, status);
CREATE INDEX review_store_id_status ON review (store_id, status);

-- A product a shopper saved for later.
CREATE TABLE wishlist (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    product_id VARCHAR(32) NOT NULL,
    store_id VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX wishlist_username ON wishlist (username);
CREATE UNIQUE INDEX wishlist_username_product_id_key ON wishlist (username, product_id);

-- An address told, by a signed POST, when something happens in a store.
CREATE TABLE webhook (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    topic VARCHAR(64) NOT NULL,
    url VARCHAR(500) NOT NULL,
    secret VARCHAR(64) NOT NULL,
    active SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX webhook_store_id_topic ON webhook (store_id, topic);

-- One attempt to deliver an event to a webhook.
CREATE TABLE webhook_delivery (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    webhook_id VARCHAR(32) NOT NULL,
    topic VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempts INTEGER NOT NULL,
    response_code INTEGER NOT NULL,
    next_attempt_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX webhook_delivery_webhook_id_created_at ON webhook_delivery (webhook_id, created_at);
CREATE INDEX webhook_delivery_status_next_attempt_at ON webhook_delivery (status, next_attempt_at);

-- Who did what in a store, for the activity log.
CREATE TABLE audit_event (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    action VARCHAR(64) NOT NULL,
    subject VARCHAR(64) NOT NULL,
    detail VARCHAR(500) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX audit_event_store_id_created_at ON audit_event (store_id, created_at);

-- A flow a merchant drew: a trigger, conditions and actions, kept as the graph's JSON.
CREATE TABLE automation (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    trigger_kind VARCHAR(64) NOT NULL,
    graph_json TEXT NOT NULL,
    active SMALLINT NOT NULL,
    runs INTEGER NOT NULL,
    last_run_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX automation_store_id_trigger_kind ON automation (store_id, trigger_kind);

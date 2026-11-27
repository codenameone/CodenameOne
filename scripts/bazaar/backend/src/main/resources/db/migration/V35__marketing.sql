-- Wave A: marketing.

-- A customer segment: a JSON rule tree compiled to a parameterized query.
CREATE TABLE audience (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    rules TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX audience_store_id ON audience (store_id);

-- An e-mail or SMS campaign to a segment, with an A/B subject test.
CREATE TABLE campaign (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    channel VARCHAR(8) NOT NULL,
    audience_id VARCHAR(32) NOT NULL,
    subject_a VARCHAR(200) NOT NULL,
    subject_b VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    ab_percent INTEGER NOT NULL,
    winner_after_minutes INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL,
    scheduled_at BIGINT NOT NULL,
    test_sent_at BIGINT NOT NULL,
    winner VARCHAR(1) NOT NULL,
    colour VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX campaign_store_id ON campaign (store_id);
CREATE INDEX campaign_status_scheduled_at ON campaign (status, scheduled_at);

-- One recipient of a campaign: which subject they got and what they did.
CREATE TABLE campaign_send (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    campaign_id VARCHAR(32) NOT NULL,
    customer_id VARCHAR(32) NOT NULL,
    address VARCHAR(190) NOT NULL,
    variant VARCHAR(1) NOT NULL,
    token VARCHAR(40) NOT NULL,
    status VARCHAR(16) NOT NULL,
    sent_at BIGINT NOT NULL,
    opened_at BIGINT NOT NULL,
    clicked_at BIGINT NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    revenue_cents BIGINT NOT NULL
);
CREATE INDEX campaign_send_campaign_id_status ON campaign_send (campaign_id, status);
CREATE INDEX campaign_send_store_id_customer_id ON campaign_send (store_id, customer_id);
CREATE UNIQUE INDEX campaign_send_u_token ON campaign_send (token);
CREATE UNIQUE INDEX campaign_send_u_campaign_id_customer_id ON campaign_send (campaign_id, customer_id);

-- A link in a campaign; a click goes through the tracker to this stored address only.
CREATE TABLE campaign_link (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    campaign_id VARCHAR(32) NOT NULL,
    position INTEGER NOT NULL,
    url VARCHAR(1000) NOT NULL
);
CREATE INDEX campaign_link_campaign_id ON campaign_link (campaign_id);

-- The per-store secret that signs unsubscribe and confirmation links.
CREATE TABLE marketing_key (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    secret VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX marketing_key_u_store_id ON marketing_key (store_id);

-- An address that unsubscribed from (or opted in to) a store's marketing on a channel.
CREATE TABLE marketing_consent (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    address VARCHAR(190) NOT NULL,
    channel VARCHAR(8) NOT NULL,
    status VARCHAR(16) NOT NULL,
    source VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX marketing_consent_store_id_address_channel ON marketing_consent (store_id, address, channel);

-- A newsletter signup form or popup: its fields, its reward and whether it double opts in.
CREATE TABLE signup_form (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    fields VARCHAR(2000) NOT NULL,
    reward_bps INTEGER NOT NULL,
    double_opt_in SMALLINT NOT NULL,
    active SMALLINT NOT NULL,
    honeypot VARCHAR(40) NOT NULL,
    headline VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX signup_form_store_id ON signup_form (store_id);

-- Someone who filled in a signup form.
CREATE TABLE signup_entry (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    form_id VARCHAR(32) NOT NULL,
    email VARCHAR(190) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    data VARCHAR(2000) NOT NULL,
    confirm_token VARCHAR(40) NOT NULL,
    status VARCHAR(16) NOT NULL,
    discount_code VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    confirmed_at BIGINT NOT NULL
);
CREATE INDEX signup_entry_form_id_email ON signup_entry (form_id, email);
CREATE UNIQUE INDEX signup_entry_u_confirm_token ON signup_entry (confirm_token);


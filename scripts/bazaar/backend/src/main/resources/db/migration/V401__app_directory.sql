-- The app directory and app billing: an app's listing and the platform's
-- decision on it, stores' reviews of apps, the charges apps ask stores for,
-- and the entries the platform pays developers from.

-- One listing per app. `search_text` is the name, tagline and description
-- folded to lowercase ASCII, so a search is one LIKE on every database.
CREATE TABLE app_listing (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    app_id VARCHAR(32) NOT NULL,
    handle VARCHAR(64) NOT NULL,
    developer VARCHAR(190) NOT NULL,
    name VARCHAR(80) NOT NULL,
    tagline VARCHAR(140) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    category VARCHAR(40) NOT NULL,
    icon_url VARCHAR(500) NOT NULL,
    screenshots VARCHAR(2600) NOT NULL,
    pricing VARCHAR(200) NOT NULL,
    support_url VARCHAR(500) NOT NULL,
    privacy_url VARCHAR(500) NOT NULL,
    search_text VARCHAR(4300) NOT NULL,
    status VARCHAR(16) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    reviewed_by VARCHAR(190) NOT NULL,
    submitted_at BIGINT NOT NULL,
    reviewed_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX app_listing_app ON app_listing (app_id);
CREATE UNIQUE INDEX app_listing_handle ON app_listing (handle);
CREATE INDEX app_listing_status ON app_listing (status, category);

-- A store's review of an app: one per store, which the unique index holds.
CREATE TABLE app_review (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    app_id VARCHAR(32) NOT NULL,
    store_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    rating INT NOT NULL,
    body VARCHAR(2000) NOT NULL,
    hidden INT NOT NULL,
    hidden_by VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX app_review_app_store ON app_review (app_id, store_id);

-- What an app asked a store for. Amounts in the currency's minor units.
CREATE TABLE app_charge (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    app_id VARCHAR(32) NOT NULL,
    installation_id VARCHAR(32) NOT NULL,
    store_id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    trial_days INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    return_url VARCHAR(500) NOT NULL,
    decided_by VARCHAR(190) NOT NULL,
    created_at BIGINT NOT NULL,
    decided_at BIGINT NOT NULL,
    billing_starts_at BIGINT NOT NULL,
    cancelled_at BIGINT NOT NULL
);
CREATE INDEX app_charge_store ON app_charge (store_id, created_at);
CREATE INDEX app_charge_installation ON app_charge (installation_id, status);

-- What the platform owes an app's developer: one line per approved charge
-- (and per recurring period billed), kept for payouts.
CREATE TABLE app_payout_entry (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    charge_id VARCHAR(32) NOT NULL,
    app_id VARCHAR(32) NOT NULL,
    store_id VARCHAR(32) NOT NULL,
    developer VARCHAR(190) NOT NULL,
    amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    due_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX app_payout_entry_app ON app_payout_entry (app_id, due_at);

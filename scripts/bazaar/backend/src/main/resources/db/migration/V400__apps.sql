-- Apps: a developer's registration, a store's installation of it, and the
-- one-time codes of the OAuth install flow between them. Secrets, tokens and
-- codes are kept as SHA-256 only.

-- An app a developer registered. `id` is short (12 hex digits) because it
-- names the app's metafield namespace, `app--<id>--`. `signing_key` is the
-- HMAC of the client secret its webhooks are signed with, which the app
-- derives from the secret it holds.
CREATE TABLE app (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    developer VARCHAR(190) NOT NULL,
    client_id VARCHAR(64) NOT NULL,
    secret_hash VARCHAR(64) NOT NULL,
    signing_key VARCHAR(64) NOT NULL,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    icon_url VARCHAR(500) NOT NULL,
    app_url VARCHAR(500) NOT NULL,
    webhook_url VARCHAR(500) NOT NULL,
    redirect_uris VARCHAR(2000) NOT NULL,
    scopes VARCHAR(1000) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX app_client_id ON app (client_id);
CREATE INDEX app_developer ON app (developer);

-- One app in one store: the scopes the owner granted and the token they
-- were granted to. Removing the app clears the token's hash; installing it
-- again reuses the row.
CREATE TABLE app_installation (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    app_id VARCHAR(32) NOT NULL,
    store_id VARCHAR(32) NOT NULL,
    scopes VARCHAR(1000) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    token_last_four VARCHAR(8) NOT NULL,
    status VARCHAR(16) NOT NULL,
    installed_by VARCHAR(190) NOT NULL,
    installed_at BIGINT NOT NULL,
    uninstalled_at BIGINT NOT NULL,
    shop_redact_due_at BIGINT NOT NULL,
    shop_redact_sent_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX app_installation_app_store ON app_installation (app_id, store_id);
CREATE INDEX app_installation_token_hash ON app_installation (token_hash);
CREATE INDEX app_installation_store ON app_installation (store_id);

-- An authorization code the owner's consent produced: good once, for ten
-- minutes, for this app, store, redirect address, scopes and PKCE challenge.
CREATE TABLE app_grant (
    code_hash VARCHAR(64) NOT NULL PRIMARY KEY,
    app_id VARCHAR(32) NOT NULL,
    store_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    scopes VARCHAR(1000) NOT NULL,
    redirect_uri VARCHAR(500) NOT NULL,
    code_challenge VARCHAR(128) NOT NULL,
    expires_at BIGINT NOT NULL,
    used_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX app_grant_expires ON app_grant (expires_at);

-- A webhook an app subscribed (or the mandatory ones every installation
-- gets); empty for the store's own.
ALTER TABLE webhook ADD COLUMN app_installation_id VARCHAR(32) NOT NULL DEFAULT '';
CREATE INDEX webhook_app_installation ON webhook (app_installation_id);

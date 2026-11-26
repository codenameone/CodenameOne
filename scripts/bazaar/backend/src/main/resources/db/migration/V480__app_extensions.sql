-- App UI extensions: declarative blocks an installed app uploaded for a
-- target (an admin page, the storefront, the checkout). `tree` is the
-- validated block tree as JSON (`ops.Blocks`); nothing in it runs. The app
-- may give a `data_url` the server asks, signed, for the values of the
-- tree's placeholders, and an `action_url` its action buttons post to.
-- They belong to the installation that uploaded them and are deleted with it.
CREATE TABLE app_extension (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    app_installation_id VARCHAR(32) NOT NULL,
    app_id VARCHAR(32) NOT NULL,
    target VARCHAR(32) NOT NULL,
    place VARCHAR(32) NOT NULL,
    title VARCHAR(80) NOT NULL,
    tree TEXT NOT NULL,
    data_url VARCHAR(500) NOT NULL,
    action_url VARCHAR(500) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX app_extension_store ON app_extension (store_id, target);
CREATE INDEX app_extension_installation ON app_extension (app_installation_id);

-- An app's storefront proxy: `/s/{handle}/apps/{subpath}/...` is forwarded,
-- server side and signed, to `url`. One subpath per store.
CREATE TABLE app_proxy (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    app_installation_id VARCHAR(32) NOT NULL,
    app_id VARCHAR(32) NOT NULL,
    subpath VARCHAR(40) NOT NULL,
    url VARCHAR(500) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX app_proxy_store_subpath ON app_proxy (store_id, subpath);
CREATE INDEX app_proxy_installation ON app_proxy (app_installation_id);

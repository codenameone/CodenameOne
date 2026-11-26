-- Checkout functions an installed app uploaded: a script compiled once at
-- upload (`program`, the verified opcode array and constant pools as JSON,
-- beside the `script` it came from) or the HTTPS address of a remote one.
-- They belong to the installation that uploaded them and run only while it
-- is installed with write_functions.
CREATE TABLE checkout_function (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    app_installation_id VARCHAR(32) NOT NULL,
    app_id VARCHAR(32) NOT NULL,
    name VARCHAR(80) NOT NULL,
    target VARCHAR(16) NOT NULL,
    kind VARCHAR(8) NOT NULL,
    script TEXT NOT NULL,
    program TEXT NOT NULL,
    url VARCHAR(500) NOT NULL,
    active SMALLINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX checkout_function_store ON checkout_function (store_id, active);
CREATE INDEX checkout_function_installation ON checkout_function (app_installation_id);

-- Staff security: whether a store requires every member of its staff to
-- sign in with an authenticator app, and the OpenID Connect providers its
-- staff may sign in through.
CREATE TABLE staff_security (
    store_id VARCHAR(32) NOT NULL PRIMARY KEY,
    require_two_factor SMALLINT NOT NULL,
    updated_at BIGINT NOT NULL
);

-- An OpenID Connect provider a store's staff sign in with. The endpoints are
-- read from the issuer's discovery document when the provider is saved, and
-- domain_roles maps the e-mail domains it vouches for to a staff role, one
-- "domain=ROLE" pair per line.
CREATE TABLE sso_provider (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    name VARCHAR(80) NOT NULL,
    issuer VARCHAR(400) NOT NULL,
    client_id VARCHAR(200) NOT NULL,
    client_secret VARCHAR(400) NOT NULL,
    authorization_uri VARCHAR(400) NOT NULL,
    token_uri VARCHAR(400) NOT NULL,
    jwks_uri VARCHAR(400) NOT NULL,
    domain_roles VARCHAR(1000) NOT NULL,
    enabled SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX sso_provider_store_id ON sso_provider (store_id);

-- Who a provider's user is here: the issuer and subject the provider
-- asserted, never the e-mail address alone.
CREATE TABLE sso_identity (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    provider_id VARCHAR(32) NOT NULL,
    issuer VARCHAR(400) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    username VARCHAR(200) NOT NULL,
    created_at BIGINT NOT NULL,
    last_login_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX sso_identity_provider_subject ON sso_identity (provider_id, subject);

-- What the bank keeps of a customer beside the sign-in tables the security
-- layer creates (cn1_users, cn1_authorities, the token, authenticator-app and
-- passkey tables). Keyed by the same username.
CREATE TABLE profile (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL,
    created_at BIGINT NOT NULL
);

-- Wrong passwords per account, for the temporary lockout. A row exists only
-- for an account that exists, so guessing at made-up names cannot grow it.
CREATE TABLE login_failure (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    failures INTEGER NOT NULL,
    first_at BIGINT NOT NULL,
    locked_until BIGINT NOT NULL
);

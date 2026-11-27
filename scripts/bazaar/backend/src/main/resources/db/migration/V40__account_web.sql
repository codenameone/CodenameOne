-- The web account area.

-- A signed-in browser session, for the security page's list of sign-ins and
-- "sign out everywhere else". Keyed by a SHA-256 of the session id, never the
-- id itself: this table must not be a list of live cookies.
CREATE TABLE web_session (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(190) NOT NULL,
    agent VARCHAR(200) NOT NULL,
    address VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    seen_at BIGINT NOT NULL,
    revoked SMALLINT NOT NULL
);
CREATE INDEX web_session_username_seen_at ON web_session (username, seen_at);

-- The account pages list a shopper's own reviews and stock alerts.
CREATE INDEX review_username_created_at ON review (username, created_at);
CREATE INDEX stock_alert_username_created_at ON stock_alert (username, created_at);

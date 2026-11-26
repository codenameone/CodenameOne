-- A shopper's account lists their own returns, newest first.
CREATE INDEX return_request_username_created_at ON return_request (username, created_at);

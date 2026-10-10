-- The pictures a driving application is backed by -- an identity document, a
-- licence, a selfie, the car's registration and its insurance -- one of each
-- kind for an account, as base64 text.
--
-- In the database and not on a disk beside the server, so every instance of
-- the server reads the same documents and a backup of the database is a backup
-- of them. The server caps what it accepts well below what this column holds.
--
-- This is the one script written per engine: a document outgrows the 64 KB of
-- MySQL's TEXT, and LONGTEXT is a type the other two do not have.
CREATE TABLE wl_driver_document (
    username VARCHAR(190) NOT NULL,
    kind VARCHAR(24) NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    uploaded_at BIGINT NOT NULL,
    data TEXT NOT NULL,
    PRIMARY KEY (username, kind)
);

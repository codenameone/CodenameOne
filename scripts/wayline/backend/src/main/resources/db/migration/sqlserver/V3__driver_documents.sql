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
CREATE TABLE driver_document (
    username NVARCHAR(190) COLLATE Latin1_General_100_BIN2 NOT NULL,
    kind NVARCHAR(24) COLLATE Latin1_General_100_BIN2 NOT NULL,
    content_type NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    status NVARCHAR(16) COLLATE Latin1_General_100_BIN2 NOT NULL,
    uploaded_at BIGINT NOT NULL,
    data NVARCHAR(MAX) COLLATE Latin1_General_100_BIN2 NOT NULL,
    PRIMARY KEY (username, kind)
);

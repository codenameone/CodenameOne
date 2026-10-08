-- The table SecureApi reads. This server only ever runs on SQLite, where an
-- INTEGER PRIMARY KEY is filled in for a row inserted without one.
CREATE TABLE secure_notes (
    id INTEGER PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    body VARCHAR(400) NOT NULL
);

// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::migrations-sql-001[]
-- V1__create_note.sql
CREATE TABLE note (
    id INTEGER PRIMARY KEY,
    body VARCHAR(2000) NOT NULL
);
CREATE INDEX note_body ON note (body);
// end::migrations-sql-001[]

// tag::migrations-sql-002[]
-- V2__add_note_created.sql
ALTER TABLE note ADD COLUMN created BIGINT;
UPDATE note SET created = 0 WHERE created IS NULL;
// end::migrations-sql-002[]

// tag::migrations-sql-003[]
-- V7__rebuild_note.sql, with V7__rebuild_note.sql.conf holding executeInTransaction=false
PRAGMA foreign_keys = OFF;
BEGIN;
CREATE TABLE note_new (id INTEGER PRIMARY KEY, body TEXT NOT NULL, created INTEGER NOT NULL DEFAULT 0);
INSERT INTO note_new (id, body, created) SELECT id, body, COALESCE(created, 0) FROM note;
DROP TABLE note;
ALTER TABLE note_new RENAME TO note;
COMMIT;
PRAGMA foreign_keys = ON;
// end::migrations-sql-003[]

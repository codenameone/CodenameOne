-- What GET /api/secure/notes answers on a fresh server. The app's tests assert on
-- these three rows exactly, so a change here is a change to those tests.
INSERT INTO secure_notes (id, title, body) VALUES (1, 'first', 'seeded by migration V2');
INSERT INTO secure_notes (id, title, body) VALUES (2, 'second', 'migrations ran before the first request');
INSERT INTO secure_notes (id, title, body) VALUES (3, 'third', 'the same rows on the JVM and in the native server');

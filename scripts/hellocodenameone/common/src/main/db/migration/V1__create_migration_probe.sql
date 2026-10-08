-- The first schema this sample ever had. Read on the device by DatabaseMigrationTest.
CREATE TABLE cn1ss_mig_note (id INTEGER PRIMARY KEY, body TEXT NOT NULL);
INSERT INTO cn1ss_mig_note (id, body) VALUES (1, 'semi;colon');
INSERT INTO cn1ss_mig_note (id, body) VALUES (2, 'second');

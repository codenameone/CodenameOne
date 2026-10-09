ALTER TABLE cn1ss_mig_note ADD COLUMN created INTEGER;
UPDATE cn1ss_mig_note SET created = id * 10;

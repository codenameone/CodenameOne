DROP VIEW IF EXISTS cn1ss_mig_summary;
CREATE VIEW cn1ss_mig_summary AS SELECT COUNT(*) AS notes, SUM(created) AS total FROM cn1ss_mig_note;

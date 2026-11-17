-- Photos and signatures are Base64 text of a few hundred kilobytes each, more
-- than MySQL's TEXT holds; each database gets the type that fits, so this
-- script is one per dialect.

CREATE TABLE job_photo (
    id VARCHAR(40) NOT NULL PRIMARY KEY,
    job_id VARCHAR(40) NOT NULL,
    taken_at BIGINT NOT NULL,
    note VARCHAR(500) NOT NULL,
    mime VARCHAR(32) NOT NULL,
    data LONGTEXT NOT NULL,
    diagnosis LONGTEXT NOT NULL
);

CREATE INDEX job_photo_job ON job_photo (job_id, taken_at);

CREATE TABLE signature (
    job_id VARCHAR(40) NOT NULL PRIMARY KEY,
    signer_name VARCHAR(120) NOT NULL,
    png LONGTEXT NOT NULL,
    signed_at BIGINT NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    report_hash VARCHAR(64) NOT NULL
);

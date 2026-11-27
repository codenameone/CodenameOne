-- VIES consultations: what the European Commission's service answered for a
-- company's VAT number and when, and a cache of answers by number so a busy
-- back office does not ask again for every save.
ALTER TABLE company ADD COLUMN vat_status VARCHAR(16) NOT NULL DEFAULT '';
ALTER TABLE company ADD COLUMN vat_name VARCHAR(200) NOT NULL DEFAULT '';
ALTER TABLE company ADD COLUMN vat_reference VARCHAR(64) NOT NULL DEFAULT '';
ALTER TABLE company ADD COLUMN vat_checked_at BIGINT NOT NULL DEFAULT 0;
CREATE TABLE vat_lookup (
    id VARCHAR(20) NOT NULL PRIMARY KEY,
    status VARCHAR(16) NOT NULL,
    name VARCHAR(200) NOT NULL,
    reference VARCHAR(64) NOT NULL,
    checked_at BIGINT NOT NULL
);

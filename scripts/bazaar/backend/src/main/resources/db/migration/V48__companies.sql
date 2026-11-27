-- Wave B: B2B companies, their sites and buyers.
CREATE TABLE company (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    vat_id VARCHAR(20) NOT NULL,
    terms_days INTEGER NOT NULL,
    price_list_id VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE TABLE company_site (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    company_id VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    line1 VARCHAR(200) NOT NULL,
    city VARCHAR(120) NOT NULL,
    postal VARCHAR(20) NOT NULL,
    country VARCHAR(2) NOT NULL
);
CREATE TABLE company_buyer (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    company_id VARCHAR(32) NOT NULL,
    username VARCHAR(190) NOT NULL,
    site_id VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX company_store_id ON company (store_id);
CREATE INDEX company_site_company_id ON company_site (company_id);
CREATE INDEX company_site_store_id ON company_site (store_id);
CREATE INDEX company_buyer_company_id ON company_buyer (company_id);
CREATE INDEX company_buyer_store_id_username ON company_buyer (store_id, username);

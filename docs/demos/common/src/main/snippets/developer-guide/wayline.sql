// Snippets of the Wayline chapters of the developer guide. Edit them here, not inline.

// tag::wayline-sql-column[]
-- V6__child_seat.sql
-- A rider may ask for a child seat. No ride has one until someone asks.
ALTER TABLE wl_ride ADD COLUMN child_seat SMALLINT NOT NULL DEFAULT 0;
// end::wayline-sql-column[]

// tag::wayline-sql-table[]
-- Companies that ride on account and are invoiced at the end of the month.
CREATE TABLE wl_account_customer (
    id VARCHAR(40) NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    billing_email VARCHAR(190) NOT NULL,
    active SMALLINT NOT NULL,
    created_at BIGINT NOT NULL
);
ALTER TABLE wl_ride ADD COLUMN account_customer VARCHAR(40) NOT NULL DEFAULT '';
// end::wayline-sql-table[]

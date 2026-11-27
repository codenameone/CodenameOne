-- Wave C: granular staff permissions, assigned per staff member per location.
CREATE TABLE permission_set (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    name VARCHAR(100) NOT NULL,
    grants VARCHAR(400) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX permission_set_store ON permission_set (store_id);
CREATE TABLE staff_permission (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    staff_id VARCHAR(32) NOT NULL,
    location_id VARCHAR(32) NOT NULL,
    set_id VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE UNIQUE INDEX staff_permission_key ON staff_permission (staff_id, location_id);
CREATE INDEX staff_permission_store ON staff_permission (store_id);

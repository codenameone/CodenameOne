-- The prepaid return label bought when a return is approved.
ALTER TABLE return_request ADD COLUMN return_carrier VARCHAR(32) NOT NULL DEFAULT '';
ALTER TABLE return_request ADD COLUMN return_tracking VARCHAR(64) NOT NULL DEFAULT '';
ALTER TABLE return_request ADD COLUMN return_label_url VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE return_request ADD COLUMN return_label_cents BIGINT NOT NULL DEFAULT 0;

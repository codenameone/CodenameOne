-- A removed payee is kept, flagged, rather than deleted: its id is what the
-- cooling-off sums payments by, and its creation time is when that period
-- started. Deleting the row let a customer remove a payee and add it again
-- to start a fresh allowance; adding it again now restores this row.
ALTER TABLE payee ADD COLUMN removed SMALLINT DEFAULT 0 NOT NULL;

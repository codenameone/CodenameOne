-- A real carrier's printable label, kept with the parcel.
ALTER TABLE fulfillment ADD COLUMN label_url VARCHAR(500) NOT NULL DEFAULT '';

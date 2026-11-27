-- A translation remembers a stamp of the original it was written against, so
-- the editor can say a field is outdated once the original changes. Empty
-- for rows written before the stamp existed: those count as translated.
ALTER TABLE translation ADD COLUMN source_hash VARCHAR(16) NOT NULL DEFAULT '';

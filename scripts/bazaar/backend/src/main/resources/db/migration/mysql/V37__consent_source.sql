-- A consent row names what set it ("form:<id>", "campaign:<id>"), and an id
-- alone is 32 characters: the 32 V35 gave the column fits no source at all.
ALTER TABLE marketing_consent MODIFY source VARCHAR(80) NOT NULL;

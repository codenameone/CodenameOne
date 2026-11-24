-- Nothing to do here: Oracle compares text exactly by default, so a key
-- keeps case and trailing spaces.
-- The version exists for mysql/V8__collation.sql, and every version
-- needs a script for each engine the server runs on.
SELECT 1 FROM DUAL;

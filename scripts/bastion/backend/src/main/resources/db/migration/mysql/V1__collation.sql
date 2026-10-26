-- Makes this database's text case sensitive and NO PAD before any table
-- exists, so every table the later scripts create inherits it. The ORM needs
-- that of a text key, and a ledger needs it twice over: an idempotency key
-- "Ab" and "ab " are two different requests. MySQL and MariaDB share no name
-- for that collation, so each family reads its own executable comment; see
-- scripts/savor's V10__collation.sql for the long story.
ALTER DATABASE CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;

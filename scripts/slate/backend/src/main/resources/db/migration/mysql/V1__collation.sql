-- Makes this database's text case sensitive and NO PAD before any table
-- exists, so every table the later scripts create inherits it. The ORM needs
-- that of a text key: "A" and "a" are two keys, and "token " keeps its
-- trailing space, as SQLite and PostgreSQL do. MySQL and MariaDB share no
-- name for that collation, so each family reads its own executable comment;
-- see scripts/savor's V10__collation.sql for the long story.
ALTER DATABASE CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;

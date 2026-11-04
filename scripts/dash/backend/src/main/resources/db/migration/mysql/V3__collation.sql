-- Makes this database's text case sensitive and NO PAD: the tables the
-- earlier scripts created, and the default every later table inherits.
--
-- The ORM needs that of a text key: "A" and "a" are two keys, and "token "
-- keeps its trailing space -- what SQLite and PostgreSQL do. MySQL's default,
-- utf8mb4_0900_ai_ci, folds both away, and the server refuses to start on a
-- key column that uses it.
--
-- The latest version, not the first: a database that already ran the earlier
-- scripts takes a new one only when it sorts after them, and its tables are
-- already there to convert. On a new database this runs after them too, and
-- converts the tables they have just created.
--
-- MySQL and MariaDB share no name for that collation (utf8mb4_bin, which both
-- have, pads), and this directory serves both. The comments are executable
-- ones, each read by one family only: MySQL runs /*!80000 ...*/ and skips the
-- MariaDB-only /*M! ...*/; MariaDB runs /*M!100400 ...*/ and skips MySQL's 8.0
-- comments.
ALTER DATABASE CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE admin_action CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE flag CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE game_ticket CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE race_record CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE race_entry CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE score CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE crew CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE crew_member CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE profile CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE push_device CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE store_purchase CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;

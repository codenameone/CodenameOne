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
ALTER TABLE closed_account CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE driver_application CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE driver_document CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE driver_hours CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE driver_state CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE live_ticket CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE moderation_event CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE payment CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE payment_customer CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE payment_method CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE payment_setup CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE payout CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE payout_account CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE phone_code CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE preference CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE pricing CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE profile CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE push_device CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE ride CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE ride_activity CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE ride_event CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE ride_passed CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;

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
ALTER TABLE ad_campaign CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE ad_event CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE answer CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE blocked_item CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE certificate CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE chapter CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE closed_account CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE coupon CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE coupon_redemption CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE device CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE download CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE frame_hash CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE genre CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE household CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE interaction CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE invoice CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE job CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE list_entry CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE moderation_event CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE moderation_item CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE payment_customer CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE payment_method CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE payment_setup CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE plan CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE playback CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE profile CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE progress CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE push_device CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE rental CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE setting CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE sharing_alert CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE subscription CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE title CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE title_rating CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE upload CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE video CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE video_key CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;

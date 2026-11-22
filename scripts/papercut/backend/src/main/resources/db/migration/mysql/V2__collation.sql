-- Usernames and display names compare exactly, the way they do on the other
-- databases: binary and NO PAD, never MySQL's case-insensitive default.
ALTER DATABASE CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE profile CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE score_run CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE run_ticket CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE flag CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE friend CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE daily_seed CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE ad_nonce CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE broadcast CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE push_device CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE store_purchase CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE admin_action CONVERT TO CHARACTER SET utf8mb4 COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;

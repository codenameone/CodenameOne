-- tag::backend-mysql-collation[]
ALTER DATABASE CHARACTER SET utf8mb4
    COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
ALTER TABLE account CONVERT TO CHARACTER SET utf8mb4
    COLLATE /*!80000 utf8mb4_0900_bin */ /*M!100400 utf8mb4_nopad_bin */;
-- end::backend-mysql-collation[]

-- Fresh installs: statement existed in legacy DBs before Flyway; V6 normalizes columns.
CREATE TABLE IF NOT EXISTS `statement` (
    `VERSION` int NOT NULL DEFAULT 0,
    `CREATEUSER` varchar(255) DEFAULT NULL,
    `CREATETIME` datetime(6) DEFAULT NULL,
    `UPDATEUSER` varchar(255) DEFAULT NULL,
    `UPDATETIME` datetime(6) DEFAULT NULL,
    `ID` varchar(255) NOT NULL,
    `BILL_FILE_NAME` varchar(512) DEFAULT NULL,
    `BILL_DATA` longtext,
    `BILL_ITEMS_NUMBER` int DEFAULT NULL,
    PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Fresh installs: house_rent existed in legacy DBs; V8 migrates rows and creates v_legacy_house_rent view.
CREATE TABLE IF NOT EXISTS house_rent (
    VERSION int NOT NULL DEFAULT 0,
    CREATEUSER varchar(255) NULL,
    CREATETIME datetime NULL,
    UPDATEUSER varchar(255) NULL,
    UPDATETIME datetime NULL,
    ID varchar(255) NOT NULL,
    CARD_ID varchar(255) NULL,
    TRANSACTION_DATE datetime NULL,
    TRANSACTION_DESC text NULL,
    BALANCE_CURRENCY varchar(20) NULL,
    BALANCE_MONEY decimal(19,4) NULL,
    CARD_TYPE_ID int NULL,
    CARD_TYPE_NAME varchar(255) NULL,
    DELETED int NULL,
    DEMOAREA text NULL,
    PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

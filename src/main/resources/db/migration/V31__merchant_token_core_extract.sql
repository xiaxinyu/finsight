-- Extract payee segment from channel-prefixed descriptions before token normalization.
-- Keep in sync with MerchantNormalizer.merchantCoreRaw().
-- v_transaction_analytics already calls finsight_normalize_merchant_token(); no view change needed.

DROP FUNCTION IF EXISTS finsight_normalize_merchant_token;

CREATE FUNCTION finsight_normalize_merchant_token(raw_merchant VARCHAR(512))
RETURNS VARCHAR(128)
DETERMINISTIC
NO SQL
RETURN IF(
    raw_merchant IS NULL OR TRIM(raw_merchant) = '',
    '',
    TRIM(
        REGEXP_REPLACE(
            REGEXP_REPLACE(
                REGEXP_REPLACE(
                    REGEXP_REPLACE(
                        REGEXP_REPLACE(
                            REGEXP_REPLACE(
                                REGEXP_REPLACE(
                                    LOWER(
                                        IF(
                                            TRIM(raw_merchant) REGEXP ' - ',
                                            TRIM(REGEXP_REPLACE(
                                                REGEXP_REPLACE(TRIM(raw_merchant), '^\\\\(消费\\\\)[[:space:]]*', ''),
                                                '^.* - ',
                                                ''
                                            )),
                                            TRIM(REGEXP_REPLACE(TRIM(raw_merchant), '^\\\\(消费\\\\)[[:space:]]*', ''))
                                        )
                                    ),
                                    '(订单|order[[:space:]]*no\\.?[[:space:]]*:?[[:space:]]*|ord(er)?[[:space:]]*[#:]?[[:space:]]*)[0-9]{4,}',
                                    '',
                                    1, 0, 'i'
                                ),
                                '(门店|store|branch|shop)[[:space:]]*[#:]?[[:space:]]*[0-9]{2,}',
                                '',
                                1, 0, 'i'
                            ),
                            '(alipay|wechat[[:space:]]*pay|wxpay|tenpay|unionpay|银联|支付宝|微信支付|财付通)',
                            '',
                            1, 0, 'i'
                        ),
                        '[[:space:]]+[0-9]{4,}$',
                        '',
                        1, 0, 'i'
                    ),
                    '[[:space:]]+(trip|trips|ride|rides|monthly|annual|subscription|mktp)$',
                    '',
                    1, 0, 'i'
                ),
                '\\.(com|cn|net|io)$',
                '',
                1, 0, 'i'
            ),
            '[[:space:]]+',
            ' ',
            1, 0, 'i'
        )
    )
);

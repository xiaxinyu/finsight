package com.finsight.application.importer;

import com.finsight.application.importer.impl.AbcTransactionStatementImporter;
import com.finsight.application.importer.impl.CcbTransactionStatementImporter;
import com.finsight.application.importer.impl.CcbDebitTransactionStatementImporter;
import com.finsight.application.importer.impl.CrbankDebitTransactionStatementImporter;
import com.finsight.application.importer.impl.CmbDebitTransactionStatementImporter;
import com.finsight.application.importer.impl.CgbCreditTransactionStatementImporter;
import com.finsight.application.importer.impl.AlipayWeChatCsvImporter;

/**
 * Selects a {@link StatementImporter} implementation from bank and card type codes.
 * Unknown combinations fall back to {@link CcbTransactionStatementImporter} (legacy default).
 */
public final class StatementImporterFactory {

    private StatementImporterFactory() {
    }

    public static StatementImporter get(String bankCode, String cardTypeCode) {
        if ("CCB".equalsIgnoreCase(bankCode)) {
            if ("credit".equalsIgnoreCase(cardTypeCode)) {
                return new CcbTransactionStatementImporter();
            } else if ("debit".equalsIgnoreCase(cardTypeCode)) {
                return new CcbDebitTransactionStatementImporter();
            }
        } else if ("CGB".equalsIgnoreCase(bankCode) || "GFB".equalsIgnoreCase(bankCode) || "GDB".equalsIgnoreCase(bankCode) || "广发银行".equalsIgnoreCase(bankCode)) {
            if ("credit".equalsIgnoreCase(cardTypeCode)) {
                return new CgbCreditTransactionStatementImporter();
            }
        } else if ("CRBANK".equalsIgnoreCase(bankCode)) {
            if ("debit".equalsIgnoreCase(cardTypeCode)) {
                return new CrbankDebitTransactionStatementImporter();
            }
        } else if ("CMB".equalsIgnoreCase(bankCode) || "招商银行".equalsIgnoreCase(bankCode)) {
            if ("debit".equalsIgnoreCase(cardTypeCode) || "credit".equalsIgnoreCase(cardTypeCode)) {
                return new CmbDebitTransactionStatementImporter();
            }
        } else if ("ABC".equalsIgnoreCase(bankCode) || "农业银行".equalsIgnoreCase(bankCode)) {
            return new AbcTransactionStatementImporter();
        } else if ("ALIPAY".equalsIgnoreCase(bankCode) || "WECHAT".equalsIgnoreCase(bankCode)) {
            return new AlipayWeChatCsvImporter();
        }
        return new CcbTransactionStatementImporter();
    }
}

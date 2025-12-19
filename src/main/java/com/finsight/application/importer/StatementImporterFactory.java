package com.finsight.application.importer;

import com.finsight.application.importer.impl.CcbTransactionStatementImporter;
import com.finsight.application.importer.impl.CcbDebitTransactionStatementImporter;
import com.finsight.application.importer.impl.CrbankDebitTransactionStatementImporter;
import com.finsight.application.importer.impl.CmbDebitTransactionStatementImporter;

public class StatementImporterFactory {
    public static StatementImporter get(String bankCode, String cardTypeCode) {
        if ("CCB".equalsIgnoreCase(bankCode)) {
            if ("credit".equalsIgnoreCase(cardTypeCode)) {
                return new CcbTransactionStatementImporter();
            } else if ("debit".equalsIgnoreCase(cardTypeCode)) {
                return new CcbDebitTransactionStatementImporter();
            }
        } else if ("CRBANK".equalsIgnoreCase(bankCode)) {
            if ("debit".equalsIgnoreCase(cardTypeCode)) {
                return new CrbankDebitTransactionStatementImporter();
            }
        } else if ("CMB".equalsIgnoreCase(bankCode) || "招商银行".equalsIgnoreCase(bankCode)) {
            if ("debit".equalsIgnoreCase(cardTypeCode)) {
                return new CmbDebitTransactionStatementImporter();
            }
        }
        return new CcbTransactionStatementImporter();
    }
}

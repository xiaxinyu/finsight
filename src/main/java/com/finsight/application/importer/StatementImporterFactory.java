package com.finsight.application.importer;

import com.finsight.application.importer.impl.CcbTransactionStatementImporter;
import com.finsight.application.importer.impl.CcbDebitTransactionStatementImporter;
import com.finsight.application.importer.impl.CrbankDebitTransactionStatementImporter;

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
        }
        return new CcbTransactionStatementImporter();
    }
}

package com.finsight.application.statement;

import com.finsight.domain.model.Transaction;

import java.util.List;

public interface StatementProcessingService {
    List<Transaction> parseAndEnrichTransactions(
            List<String[]> dataRows,
            String bankCode,
            String cardTypeCode,
            String cardNo,
            String bankCardId,
            String statementId);

    void savePreviewTemps(String statementId, List<Transaction> transactions, String userName);
}

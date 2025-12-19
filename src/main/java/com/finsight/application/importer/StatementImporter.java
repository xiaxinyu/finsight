package com.finsight.application.importer;

import com.finsight.domain.model.Transaction;

import java.util.List;

public interface StatementImporter {
    List<Transaction> parse(List<String[]> rows, String bankCode, String cardTypeCode, String cardNo);
}

package com.finsight.domain.port;

import com.finsight.domain.model.KeyValue;
import com.finsight.domain.model.Page;
import com.finsight.domain.model.Transaction;
import com.finsight.application.query.TransactionQuery;

import java.util.List;

public interface TransactionRepository {
    void updateTransaction(Transaction transaction);

    void deleteTransaction(String id);

    int incomeToExpense(List<String> ids, String updateUser);

    int expenseToIncome(List<String> ids, String updateUser);

    int countTransaction(TransactionQuery query);

    List<Transaction> getTransactions(TransactionQuery query, Page page);

    Transaction selectById(String id);

    void insert(Transaction transaction);

    int deleteByStatementId(String statementId);

    List<KeyValue> consumeReport(TransactionQuery query);

    List<KeyValue> weekConsumeReport(TransactionQuery query);

    List<KeyValue> monthConsumeReport(TransactionQuery query);

    List<KeyValue> monthIncomeReport(TransactionQuery query);

    List<KeyValue> monthExpenseReport(TransactionQuery query);

    List<KeyValue> homeSummaryExpenseBuckets(Integer year);

    List<KeyValue> homeSummaryExpenseBucketsPrev(Integer year);

    Double sumIncomeByYear(Integer year);

    Double sumDebtPaymentsByYear(Integer year);

    Integer countRefundsByYear(Integer year);
}


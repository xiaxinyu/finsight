package com.finsight.domain.port;

import com.finsight.domain.model.KeyValue;
import com.finsight.domain.model.Page;
import com.finsight.domain.model.Transaction;
import com.finsight.application.query.TransactionQuery;

import java.util.List;

public interface TransactionRepository {
    void updateTransaction(Transaction transaction);

    void deleteTransaction(String id, String updateUser);

    int incomeToExpense(List<String> ids, String updateUser);

    int expenseToIncome(List<String> ids, String updateUser);

    int countTransaction(TransactionQuery query);

    List<Transaction> getTransactions(TransactionQuery query, Page page);

    Transaction selectById(String id);

    void insert(Transaction transaction);

    int deleteByStatementId(String statementId, String updateUser);

    List<com.finsight.domain.model.CategoryAggregate> consumeReport(TransactionQuery query);

    List<KeyValue> weekConsumeReport(TransactionQuery query);

    List<KeyValue> monthConsumeReport(TransactionQuery query);

    List<KeyValue> monthIncomeReport(TransactionQuery query);

    List<KeyValue> monthExpenseReport(TransactionQuery query);

    List<KeyValue> homeSummaryExpenseBuckets(Integer year, String ownerUserId);

    List<KeyValue> homeSummaryExpenseBucketsPrev(Integer year, String ownerUserId);

    List<KeyValue> homeSummaryExpenseBucketsForRange(java.util.Date start, java.util.Date end, String ownerUserId);

    Double sumIncomeForRange(java.util.Date start, java.util.Date end, String ownerUserId);

    Double sumIncomeByYear(Integer year, String ownerUserId);

    Double sumDebtPaymentsByYear(Integer year, String ownerUserId);

    Integer countRefundsByYear(Integer year, String ownerUserId);

    List<String> listIdsNeedingAmountNormalization();
}


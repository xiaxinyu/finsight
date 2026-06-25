package com.finsight.infrastructure.mapper;

import com.finsight.domain.model.Transaction;
import com.finsight.domain.model.KeyValue;
import com.finsight.domain.model.Page;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finsight.application.query.TransactionQuery;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface TransactionMapper extends BaseMapper<Transaction> {
    void addTransactionList(@Param("transactions") List<Transaction> transactions);
    void updateTransaction(Transaction transaction);
    void deleteTransaction(@Param("id") String id, @Param("updateUser") String updateUser);
    int softDeleteByStatementId(@Param("statementId") String statementId, @Param("updateUser") String updateUser);
    int incomeToExpense(@Param("ids") List<String> ids, @Param("updateUser") String updateUser);
    int expenseToIncome(@Param("ids") List<String> ids, @Param("updateUser") String updateUser);
    int countTransaction(@Param("q") TransactionQuery query);
    List<Transaction> getTransactions(@Param("q") TransactionQuery query, @Param("page") Page page);
    List<com.finsight.domain.model.CategoryAggregate> consumeReport(@Param("q") TransactionQuery query);
    List<KeyValue> weekConsumeReport(@Param("q") TransactionQuery query);
    List<KeyValue> monthConsumeReport(@Param("q") TransactionQuery query);
    List<KeyValue> monthIncomeReport(@Param("q") TransactionQuery query);
    List<KeyValue> monthExpenseReport(@Param("q") TransactionQuery query);
    List<KeyValue> homeSummaryExpenseBuckets(@Param("year") Integer year);
    Double sumIncomeByYear(@Param("year") Integer year);
    Double sumDebtPaymentsByYear(@Param("year") Integer year);
    Integer countRefundsByYear(@Param("year") Integer year);
    List<KeyValue> homeSummaryExpenseBucketsPrev(@Param("year") Integer year);

    List<KeyValue> homeSummaryExpenseBucketsForRange(@Param("start") Date start, @Param("end") Date end);

    Double sumIncomeForRange(@Param("start") Date start, @Param("end") Date end);

    List<String> listIdsNeedingAmountNormalization();

    java.util.Map<String, Object> aggregateStats(@Param("q") TransactionQuery query);

    java.util.List<com.finsight.domain.model.DrillBreakdownItem> drillCategoryBreakdown(
            @Param("q") TransactionQuery query, @Param("limit") int limit);

    java.util.List<com.finsight.domain.model.DrillBreakdownItem> drillMerchantBreakdown(
            @Param("q") TransactionQuery query, @Param("limit") int limit);
}

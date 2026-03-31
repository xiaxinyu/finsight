package com.finsight.infrastructure.mapper;

import com.finsight.domain.model.Transaction;
import com.finsight.domain.model.KeyValue;
import com.finsight.domain.model.Page;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finsight.application.query.TransactionQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TransactionMapper extends BaseMapper<Transaction> {
    void addTransactionList(@Param("transactions") List<Transaction> transactions);
    void updateTransaction(Transaction transaction);
    void deleteTransaction(String id);
    int incomeToExpense(@Param("ids") List<String> ids, @Param("updateUser") String updateUser);
    int expenseToIncome(@Param("ids") List<String> ids, @Param("updateUser") String updateUser);
    int countTransaction(@Param("q") TransactionQuery query);
    List<Transaction> getTransactions(@Param("q") TransactionQuery query, @Param("page") Page page);
    List<KeyValue> consumeReport(@Param("q") TransactionQuery query);
    List<KeyValue> weekConsumeReport(@Param("q") TransactionQuery query);
    List<KeyValue> monthConsumeReport(@Param("q") TransactionQuery query);
    List<KeyValue> monthIncomeReport(@Param("q") TransactionQuery query);
    List<KeyValue> monthExpenseReport(@Param("q") TransactionQuery query);
    List<KeyValue> homeSummaryExpenseBuckets(@Param("year") Integer year);
    Double sumIncomeByYear(@Param("year") Integer year);
    Double sumDebtPaymentsByYear(@Param("year") Integer year);
    Integer countRefundsByYear(@Param("year") Integer year);
    List<KeyValue> homeSummaryExpenseBucketsPrev(@Param("year") Integer year);
}

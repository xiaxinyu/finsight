package com.finsight.infrastructure.mapper;

import com.finsight.domain.model.Transaction;
import com.finsight.domain.model.KeyValue;
import com.finsight.domain.model.Page;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TransactionMapper extends BaseMapper<Transaction> {
    void addTransactionList(@Param("transactions") List<Transaction> transactions);
    void updateTransaction(Transaction transaction);
    void deleteTransaction(String id);
    int incomeToExpense(@Param("ids") List<String> ids, @Param("updateUser") String updateUser);
    int expenseToIncome(@Param("ids") List<String> ids, @Param("updateUser") String updateUser);
    int countTransaction(@Param("transaction") Transaction transaction);
    List<Transaction> getTransactions(@Param("transaction") Transaction transaction, @Param("page") Page page);
    List<KeyValue> consumeReport(@Param("transaction") Transaction transaction);
    List<KeyValue> weekConsumeReport(@Param("transaction") Transaction transaction);
    List<KeyValue> monthConsumeReport(@Param("transaction") Transaction transaction);
    List<KeyValue> monthIncomeReport(@Param("transaction") Transaction transaction);
    List<KeyValue> homeSummaryExpenseBuckets(@Param("year") Integer year);
    Double sumIncomeByYear(@Param("year") Integer year);
    Double sumDebtPaymentsByYear(@Param("year") Integer year);
    Integer countRefundsByYear(@Param("year") Integer year);
    List<KeyValue> homeSummaryExpenseBucketsPrev(@Param("year") Integer year);
}

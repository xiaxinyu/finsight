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
    int countTransaction(@Param("transaction") Transaction transaction);
    List<Transaction> getTransactions(@Param("transaction") Transaction transaction, @Param("page") Page page);
    List<KeyValue> consumeReport(@Param("transaction") Transaction transaction);
    List<KeyValue> weekConsumeReport(@Param("transaction") Transaction transaction);
    List<KeyValue> monthConsumeReport(@Param("transaction") Transaction transaction);
}

package com.finsight.infrastructure.repository;

import com.finsight.domain.model.KeyValue;
import com.finsight.domain.model.Page;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.application.query.TransactionQuery;
import com.finsight.infrastructure.mapper.TransactionMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TransactionMybatisRepository implements TransactionRepository {

    @Autowired
    private TransactionMapper transactionMapper;

    @Override
    public void updateTransaction(Transaction transaction) {
        transactionMapper.updateTransaction(transaction);
    }

    @Override
    public void deleteTransaction(String id) {
        transactionMapper.deleteTransaction(id);
    }

    @Override
    public int incomeToExpense(List<String> ids, String updateUser) {
        return transactionMapper.incomeToExpense(ids, updateUser);
    }

    @Override
    public int expenseToIncome(List<String> ids, String updateUser) {
        return transactionMapper.expenseToIncome(ids, updateUser);
    }

    @Override
    public int countTransaction(TransactionQuery query) {
        return transactionMapper.countTransaction(query);
    }

    @Override
    public List<Transaction> getTransactions(TransactionQuery query, Page page) {
        return transactionMapper.getTransactions(query, page);
    }

    @Override
    public Transaction selectById(String id) {
        return transactionMapper.selectById(id);
    }

    @Override
    public void insert(Transaction transaction) {
        transactionMapper.insert(transaction);
    }

    @Override
    public int deleteByStatementId(String statementId) {
        if (StringUtils.isBlank(statementId)) {
            return 0;
        }
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Transaction> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("statement_id", statementId);
        return transactionMapper.delete(wrapper);
    }

    @Override
    public List<com.finsight.domain.model.CategoryAggregate> consumeReport(TransactionQuery query) {
        return transactionMapper.consumeReport(query);
    }

    @Override
    public List<KeyValue> weekConsumeReport(TransactionQuery query) {
        return transactionMapper.weekConsumeReport(query);
    }

    @Override
    public List<KeyValue> monthConsumeReport(TransactionQuery query) {
        return transactionMapper.monthConsumeReport(query);
    }

    @Override
    public List<KeyValue> monthIncomeReport(TransactionQuery query) {
        return transactionMapper.monthIncomeReport(query);
    }

    @Override
    public List<KeyValue> monthExpenseReport(TransactionQuery query) {
        return transactionMapper.monthExpenseReport(query);
    }

    @Override
    public List<KeyValue> homeSummaryExpenseBuckets(Integer year) {
        return transactionMapper.homeSummaryExpenseBuckets(year);
    }

    @Override
    public List<KeyValue> homeSummaryExpenseBucketsPrev(Integer year) {
        return transactionMapper.homeSummaryExpenseBucketsPrev(year);
    }

    @Override
    public List<KeyValue> homeSummaryExpenseBucketsForRange(java.util.Date start, java.util.Date end) {
        return transactionMapper.homeSummaryExpenseBucketsForRange(start, end);
    }

    @Override
    public Double sumIncomeForRange(java.util.Date start, java.util.Date end) {
        return transactionMapper.sumIncomeForRange(start, end);
    }

    @Override
    public Double sumIncomeByYear(Integer year) {
        return transactionMapper.sumIncomeByYear(year);
    }

    @Override
    public Double sumDebtPaymentsByYear(Integer year) {
        return transactionMapper.sumDebtPaymentsByYear(year);
    }

    @Override
    public Integer countRefundsByYear(Integer year) {
        return transactionMapper.countRefundsByYear(year);
    }

    @Override
    public List<String> listIdsNeedingAmountNormalization() {
        return transactionMapper.listIdsNeedingAmountNormalization();
    }
}


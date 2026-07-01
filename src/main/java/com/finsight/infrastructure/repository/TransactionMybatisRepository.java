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
    public void deleteTransaction(String id, String updateUser) {
        transactionMapper.deleteTransaction(id, updateUser);
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
    public int deleteByStatementId(String statementId, String updateUser) {
        if (StringUtils.isBlank(statementId)) {
            return 0;
        }
        return transactionMapper.softDeleteByStatementId(statementId, updateUser);
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
    public List<KeyValue> homeSummaryExpenseBuckets(Integer year, String ownerUserId) {
        return transactionMapper.homeSummaryExpenseBuckets(year, ownerUserId);
    }

    @Override
    public List<KeyValue> homeSummaryExpenseBucketsPrev(Integer year, String ownerUserId) {
        return transactionMapper.homeSummaryExpenseBucketsPrev(year, ownerUserId);
    }

    @Override
    public List<KeyValue> homeSummaryExpenseBucketsForRange(java.util.Date start, java.util.Date end, String ownerUserId) {
        return transactionMapper.homeSummaryExpenseBucketsForRange(start, end, ownerUserId);
    }

    @Override
    public Double sumIncomeForRange(java.util.Date start, java.util.Date end, String ownerUserId) {
        return transactionMapper.sumIncomeForRange(start, end, ownerUserId);
    }

    @Override
    public Double sumIncomeByYear(Integer year, String ownerUserId) {
        return transactionMapper.sumIncomeByYear(year, ownerUserId);
    }

    @Override
    public Double sumDebtPaymentsByYear(Integer year, String ownerUserId) {
        return transactionMapper.sumDebtPaymentsByYear(year, ownerUserId);
    }

    @Override
    public Integer countRefundsByYear(Integer year, String ownerUserId) {
        return transactionMapper.countRefundsByYear(year, ownerUserId);
    }

    @Override
    public List<String> listIdsNeedingAmountNormalization() {
        return transactionMapper.listIdsNeedingAmountNormalization();
    }
}


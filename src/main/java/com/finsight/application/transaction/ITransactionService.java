package com.finsight.application.transaction;

import com.finsight.domain.model.Transaction;
import com.finsight.domain.model.Page;
import com.finsight.common.exception.AppServiceException;

import java.util.List;

public interface ITransactionService {
    void updateTransaction(Transaction transaction, String userName) throws AppServiceException;

    int incomeToExpense(List<String> ids, String userName) throws AppServiceException;

    int expenseToIncome(List<String> ids, String userName) throws AppServiceException;

    void deleteTransaction(String id) throws AppServiceException;

    List<Transaction> getTransactions(Transaction transaction, Page page) throws AppServiceException;

    int countTransaction(Transaction transaction) throws AppServiceException;

    void deleteByStatementId(String statementId);

    void addTransactions(List<String[]> rowDatas, String customerName, String recordID);

    int addTransactions(List<Transaction> transactions, String userName);

    String consumeReport(Transaction transaction) throws AppServiceException;

    String weekConsumeReport(Transaction transaction) throws AppServiceException;

    String monthConsumeReport(Transaction transaction) throws AppServiceException;

    String monthIncomeReport(Transaction transaction) throws AppServiceException;

    String monthExpenseReport(Transaction transaction) throws AppServiceException;

    String homeSummary(Integer year) throws AppServiceException;
}

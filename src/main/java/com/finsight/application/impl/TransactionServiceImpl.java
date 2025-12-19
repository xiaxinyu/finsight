package com.finsight.application.impl;

import com.finsight.core.DateParseException;
import com.finsight.core.DateTool;
import com.finsight.core.StringTool;
import com.finsight.infrastructure.mapper.TransactionMapper;
import com.finsight.domain.model.Card;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.model.KeyValue;
import com.finsight.domain.model.Page;
import com.finsight.application.card.CardService;
import com.finsight.core.AppException;
import com.finsight.core.AppServiceException;
import com.finsight.application.ITransactionService;
import com.alibaba.fastjson.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by Summer.Xia on 09/01/2015.
 */
@Service("transactionService")
public class TransactionServiceImpl implements ITransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);
    @Autowired
    CardService cardService;

    @Autowired
    TransactionMapper transactionMapper;

    @Override
    public void updateTransaction(Transaction transaction, String userName) throws AppServiceException {
        try {
            transaction.setUpdateUser(userName);
            transactionMapper.updateTransaction(transaction);
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
    }

    @Override
    public void deleteTransaction(String id) throws AppServiceException {
        try {
            transactionMapper.deleteTransaction(id);
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
    }

    @Override
    public List<Transaction> getTransactions(Transaction transaction, Page page) throws AppServiceException {
        List<Transaction> result = null;
        try {
            log.info("Query transactions：page={}", page);
            result = transactionMapper.getTransactions(transaction, page);
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
        return result;
    }

    @Override
    public int countTransaction(Transaction transaction) throws AppServiceException {
        int result = 0;
        try {
            result = transactionMapper.countTransaction(transaction);
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
        return result;
    }

    @Override
    public void deleteByStatementId(String statementId) {
        if (StringUtils.isBlank(statementId)) {
            return;
        }
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Transaction> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("recordID", statementId);
        transactionMapper.delete(wrapper);
    }

    @Override
    public void addTransactions(List<String[]> rowDatas, String userName, String recordID) {
        if (CollectionUtils.isEmpty(rowDatas)) {
            throw new AppException("No exist original transaction data, can't call add transactions!");
        }

        Map<String, Card> cardMap = cardService.queryAllCards();

        boolean skipTitle = true;
        for (String[] rowData : rowDatas) {
            try {
                if (skipTitle) {
                    skipTitle = false;
                    continue;
                }
                if (rowData == null || rowData.length < 6) {
                    continue;
                }

                Transaction transaction = new Transaction();
                transaction.setId(StringTool.generateID());
                transaction.setCreateUser(userName);
                transaction.setUpdateUser(userName);
                transaction.setId(StringTool.generateID());
                String cardId = StringTool.cleanStr(rowData[0]);
                transaction.setCardId(cardId);
                transaction.setTransactionDate(DateTool.changeStringToDate(StringTool.cleanStr(rowData[1]), DateTool.DF_YYYY_MM_DD));
                transaction.setBookKeepingDate(DateTool.changeStringToDate(StringTool.cleanStr(rowData[2]), DateTool.DF_YYYY_MM_DD));
                transaction.setTransactionDesc(StringTool.cleanStr(rowData[3]));
                transaction.setBalanceCurrency(StringTool.cleanStr(rowData[4]));

                String balanceMoney = StringTool.cleanStr(rowData[5]);
                transaction.setBalanceMoney(StringUtils.isBlank(balanceMoney) ? 0 : Double.parseDouble(balanceMoney));

                transaction.setCardTypeId(1);
                transaction.setCardTypeName(cardMap.get(cardId).getCardName());
                transaction.setRecordID(recordID);
                transactionMapper.insert(transaction);
            } catch (Exception e) {
                log.error("Saving transaction has error: transaction={}", StringUtils.join(rowDatas, ","), e);
            }
        }
    }

    @Override
    public int addTransactions(List<Transaction> transactions, String userName) {
        if (transactions == null || transactions.isEmpty()) {
            throw new AppException("No exist parsed transaction data");
        }
        int success = 0;
        for (Transaction transaction : transactions) {
            try {
                if (transaction.getId() == null || transaction.getId().trim().isEmpty() || transactionMapper.selectById(transaction.getId()) != null) {
                    transaction.setId(com.finsight.core.StringTool.generateID());
                }
                transaction.setCreateUser(userName);
                transaction.setUpdateUser(userName);
                transactionMapper.insert(transaction);
                success++;
            } catch (Exception e) {
                log.error("Saving transaction has error: transaction={}", transaction, e);
            }
        }
        return success;
    }

    private void fetchTransactionParam(Transaction transaction) throws DateParseException {
        if (StringTool.isNullOrEmpty(transaction.getCardTypeName())) {
            transaction.setCardTypeName(null);
        }
        if (!StringTool.isNullOrEmpty(transaction.getConsumeID())) {
            transaction.setConsumes(transaction.getConsumeID().split(","));
        }
        if (!StringTool.isNullOrEmpty(transaction.getTransactionDateStartStr())) {
            transaction.setTransactionDateStart(DateTool.changeStringToDate(transaction.getTransactionDateStartStr(), DateTool.DF_MM_DD_YYYY));
        }
        if (!StringTool.isNullOrEmpty(transaction.getTransactionDateEndStr())) {
            transaction.setTransactionDateEnd(DateTool.changeStringToDate(transaction.getTransactionDateEndStr(), DateTool.DF_MM_DD_YYYY));
        }
        if (StringTool.isNullOrEmpty(transaction.getDemoArea())) {
            transaction.setDemoArea(null);
        }
    }

    @Override
    public String consumeReport(Transaction transaction) throws AppServiceException {
        String result = StringTool.EMPTY;
        try {
            fetchTransactionParam(transaction);
            List<KeyValue> list = transactionMapper.consumeReport(transaction);
            result = JSONArray.toJSONString(list);
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
        return result;
    }

    @Override
    public String weekConsumeReport(Transaction transaction) throws AppServiceException {
        String result = StringTool.EMPTY;
        try {
            fetchTransactionParam(transaction);
            List<KeyValue> list = transactionMapper.weekConsumeReport(transaction);
            result = JSONArray.toJSONString(list);
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
        return result;
    }

    @Override
    public String monthConsumeReport(Transaction transaction) throws AppServiceException {
        String result = StringTool.EMPTY;
        try {
            fetchTransactionParam(transaction);
            List<KeyValue> list = transactionMapper.monthConsumeReport(transaction);
            result = JSONArray.toJSONString(list).toString();
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
        return result;
    }
}

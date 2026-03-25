package com.finsight.application.service.impl;

import com.finsight.application.card.BankCardService;
import com.finsight.application.consume.ClassificationService;
import com.finsight.application.importer.StatementImporterFactory;
import com.finsight.application.service.StatementProcessingService;
import com.finsight.domain.model.BankCard;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.model.TransactionTemp;
import com.finsight.application.service.ITransactionTempService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class StatementProcessingServiceImpl implements StatementProcessingService {

    @Autowired
    private BankCardService bankCardService;

    @Autowired
    private ClassificationService classificationService;

    @Autowired
    private ITransactionTempService transactionTempService;

    @Override
    public List<Transaction> parseAndEnrichTransactions(List<String[]> dataRows, String bankCode, String cardTypeCode, String cardNo, String statementId) {
        List<Transaction> transactions = StatementImporterFactory
                .get(StringUtils.trimToEmpty(bankCode), StringUtils.trimToEmpty(cardTypeCode))
                .parse(dataRows, bankCode, cardTypeCode, cardNo);
        BankCard bankCard = resolveBankCard(bankCode, cardTypeCode, cardNo);
        String bankCardId = bankCard == null ? null : bankCard.getId();
        String bankCardName = bankCard == null ? null : bankCard.getCardName();
        for (Transaction t : transactions) {
            if (StringUtils.isNotBlank(bankCardId)) {
                t.setBankCardId(bankCardId);
                t.setBankCardName(bankCardName);
            }
            classifyTransaction(t, bankCode, cardTypeCode);
            if (StringUtils.isNotBlank(statementId)) {
                t.setRecordID(statementId);
            }
        }
        return transactions;
    }

    @Override
    public void savePreviewTemps(String statementId, List<Transaction> transactions, String userName) {
        transactionTempService.deleteByStatementId(statementId);
        if (transactions == null || transactions.isEmpty()) {
            return;
        }
        List<TransactionTemp> temps = new ArrayList<>();
        for (Transaction t : transactions) {
            if (t == null) {
                continue;
            }
            TransactionTemp temp = new TransactionTemp();
            org.springframework.beans.BeanUtils.copyProperties(t, temp);
            temp.setId(UUID.randomUUID().toString());
            temp.setCreateUser(userName);
            temp.setUpdateUser(userName);
            temp.setTransactionDateTime(buildDateTime(t.getTransactionDate(), t.getTransactionTime()));
            temps.add(temp);
        }
        transactionTempService.saveBatch(temps);
    }

    private BankCard resolveBankCard(String bankCode, String cardTypeCode, String cardNo) {
        BankCard bankCard = bankCardService.getByBankTypeNo(StringUtils.trimToEmpty(bankCode), StringUtils.trimToEmpty(cardTypeCode), StringUtils.trimToEmpty(cardNo));
        if (bankCard == null && StringUtils.isNotBlank(cardNo)) {
            bankCard = bankCardService.getByCardNo(StringUtils.trimToEmpty(cardNo));
        }
        return bankCard;
    }

    private void classifyTransaction(Transaction t, String bankCode, String cardTypeCode) {
        Double amount = t.getBalanceMoney();
        if (amount != null) {
            amount = Math.abs(amount);
        }
        Date txnDate = t.getTransactionDate();
        if (txnDate == null) {
            txnDate = t.getBookKeepingDate();
        }
        ClassificationService.Result r = classificationService.classify(t.getTransactionDesc(), bankCode, cardTypeCode, amount, txnDate);
        if (r != null) {
            t.setConsumeCode(r.id);
            t.setConsumeName(r.name);
        }
    }

    private String buildDateTime(Date txnDate, String txnTime) {
        try {
            String ds = txnDate == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(txnDate);
            String ts = StringUtils.trimToEmpty(txnTime);
            return StringUtils.isNotBlank(ts) ? (ds + " " + ts) : ds;
        } catch (Exception e) {
            return "";
        }
    }
}

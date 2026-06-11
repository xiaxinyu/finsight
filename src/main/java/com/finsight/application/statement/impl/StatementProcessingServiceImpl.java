package com.finsight.application.statement.impl;

import com.finsight.application.card.BankCardService;
import com.finsight.application.transaction.TransactionAmountNormalizer;
import com.finsight.application.transaction.TransactionFieldSanitizer;
import com.finsight.application.consume.ClassificationService;
import com.finsight.application.importer.StatementImporterFactory;
import com.finsight.application.statement.StatementProcessingService;
import com.finsight.domain.model.BankCard;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.model.TransactionTemp;
import com.finsight.domain.port.TransactionTempRepository;
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
    private TransactionTempRepository transactionTempRepository;

    @Override
    public List<Transaction> parseAndEnrichTransactions(
            List<String[]> dataRows,
            String bankCode,
            String cardTypeCode,
            String cardNo,
            String bankCardId,
            String statementId) {
        List<Transaction> transactions = StatementImporterFactory
                .get(StringUtils.trimToEmpty(bankCode), StringUtils.trimToEmpty(cardTypeCode))
                .parse(dataRows, bankCode, cardTypeCode, cardNo);
        BankCard bankCard = resolveBankCard(bankCode, cardTypeCode, cardNo, bankCardId);
        String resolvedCardId = bankCard == null ? null : bankCard.getId();
        String resolvedCardName = bankCard == null ? null : bankCard.getCardName();
        for (Transaction t : transactions) {
            TransactionAmountNormalizer.normalize(t);
            TransactionFieldSanitizer.sanitize(t);
            if (StringUtils.isNotBlank(resolvedCardId)) {
                t.setBankCardId(resolvedCardId);
                t.setBankCardName(resolvedCardName);
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
        transactionTempRepository.softDeleteByStatementId(statementId);
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
        transactionTempRepository.saveBatch(temps);
    }

    private BankCard resolveBankCard(String bankCode, String cardTypeCode, String cardNo, String bankCardId) {
        if (StringUtils.isNotBlank(bankCardId)) {
            BankCard byId = bankCardService.getById(StringUtils.trim(bankCardId));
            if (byId != null && (byId.getDeleted() == null || byId.getDeleted() == 0)) {
                return byId;
            }
        }
        String b = StringUtils.trimToEmpty(bankCode).toUpperCase();
        String t = StringUtils.trimToEmpty(cardTypeCode).toLowerCase();
        String n = StringUtils.trimToEmpty(cardNo);
        if (StringUtils.isNotBlank(n)) {
            BankCard bankCard = bankCardService.getByBankTypeNo(b, t, n);
            if (bankCard == null) {
                bankCard = bankCardService.getByCardNo(n);
            }
            if (bankCard != null) {
                return bankCard;
            }
        }
        if (StringUtils.isNotBlank(b) && StringUtils.isNotBlank(t)) {
            List<BankCard> matches = bankCardService.listByBankAndType(b, t);
            if (matches != null && matches.size() == 1) {
                return matches.get(0);
            }
        }
        return null;
    }

    /**
     * Rule-based category uses a single magnitude: statement imports set {@code incomeMoney} for inflows
     * and {@code balanceMoney} as a positive expense amount for outflows (never both on one row).
     */
    private void classifyTransaction(Transaction t, String bankCode, String cardTypeCode) {
        double income = t.getIncomeMoney() == null ? 0.0 : Math.max(0.0, t.getIncomeMoney());
        double expense = t.getBalanceMoney() == null ? 0.0 : Math.max(0.0, t.getBalanceMoney());
        double amount = Math.max(income, expense);
        Date txnDate = t.getTransactionDate();
        if (txnDate == null) {
            txnDate = t.getBookKeepingDate();
        }
        ClassificationService.Result r = classificationService.classify(t.getTransactionDesc(), bankCode, cardTypeCode, amount, txnDate);
        if (r != null) {
            t.setCategoryCode(r.id);
            t.setCategoryName(r.name);
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

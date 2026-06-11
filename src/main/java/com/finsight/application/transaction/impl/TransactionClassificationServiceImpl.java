package com.finsight.application.transaction.impl;

import com.alibaba.fastjson.JSON;
import com.finsight.application.card.BankCardService;
import com.finsight.application.consume.ClassificationNarrationBuilder;
import com.finsight.application.consume.ClassificationService;
import com.finsight.application.transaction.ITransactionClassificationService;
import com.finsight.common.exception.AppServiceException;
import com.finsight.domain.model.BankCard;
import com.finsight.domain.model.Transaction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionClassificationServiceImpl implements ITransactionClassificationService {

    @Autowired
    private ClassificationService classificationService;

    @Autowired
    private BankCardService bankCardService;

    @Override
    public Optional<String> classifyForApi(Transaction transaction, String bankCode, String cardTypeCode)
            throws AppServiceException {
        return suggestTopN(transaction, bankCode, cardTypeCode);
    }

    @Override
    public Optional<String> suggestTopN(Transaction transaction, String bankCode, String cardTypeCode)
            throws AppServiceException {
        try {
            ClassificationContext ctx = buildContext(transaction, bankCode, cardTypeCode);
            List<ClassificationService.Result> rs = classificationService.classifyTopN(
                    ctx.narration(), ctx.bankCode(), ctx.cardTypeCode(), ctx.amount(), ctx.txnDate(), 5);
            if (rs == null || rs.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(JSON.toJSONString(rs));
        } catch (Exception e) {
            throw new AppServiceException(e.getMessage(), e);
        }
    }

    @Override
    public String keywordsJson(Transaction transaction) throws AppServiceException {
        try {
            String narration = ClassificationNarrationBuilder.fromTransaction(transaction);
            List<String> ks;
            try {
                ks = classificationService == null ? Collections.emptyList() : classificationService.tokens(narration);
            } catch (Exception e) {
                ks = Collections.emptyList();
            }
            if (ks == null) {
                ks = Collections.emptyList();
            }
            return JSON.toJSONString(ks);
        } catch (Exception e) {
            throw new AppServiceException(e.getMessage(), e);
        }
    }

    private ClassificationContext buildContext(Transaction transaction, String bankCode, String cardTypeCode) {
        String narration = ClassificationNarrationBuilder.fromTransaction(transaction);
        bankCode = StringUtils.trimToEmpty(bankCode);
        cardTypeCode = StringUtils.trimToEmpty(cardTypeCode);
        if (StringUtils.isBlank(bankCode) && StringUtils.isNotBlank(transaction.getBankCardId())) {
            BankCard card = bankCardService.getById(transaction.getBankCardId());
            if (card != null) {
                bankCode = StringUtils.trimToEmpty(card.getBankCode());
                if (StringUtils.isBlank(cardTypeCode)) {
                    cardTypeCode = StringUtils.trimToEmpty(card.getCardTypeCode());
                }
            }
        }
        if (!StringUtils.isNotBlank(bankCode)) {
            bankCode = StringUtils.trimToEmpty(transaction.getBankCardName());
        }
        if (!StringUtils.isNotBlank(cardTypeCode)) {
            cardTypeCode = StringUtils.trimToEmpty(transaction.getCardTypeName());
        }
        if (StringUtils.isNotBlank(cardTypeCode)) {
            cardTypeCode = cardTypeCode.trim().toLowerCase();
        }
        double income = transaction.getIncomeMoney() == null ? 0.0 : Math.max(0.0, transaction.getIncomeMoney());
        double expense = transaction.getBalanceMoney() == null ? 0.0 : Math.max(0.0, transaction.getBalanceMoney());
        Double amount = Math.max(income, expense);
        Date txnDate = transaction.getTransactionDate();
        if (txnDate == null) {
            txnDate = transaction.getBookKeepingDate();
        }
        if (txnDate == null) {
            String dt = StringUtils.trimToEmpty(transaction.getTransactionDateTime());
            if (StringUtils.isNotBlank(dt)) {
                try {
                    ParsePosition pos = new ParsePosition(0);
                    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date d = f.parse(dt, pos);
                    if (d == null) {
                        f = new SimpleDateFormat("yyyy-MM-dd");
                        d = f.parse(dt, new ParsePosition(0));
                    }
                    txnDate = d;
                } catch (Exception ignore) {
                    // leave txnDate null
                }
            }
        }
        return new ClassificationContext(narration, bankCode, cardTypeCode, amount, txnDate);
    }

    private record ClassificationContext(
            String narration, String bankCode, String cardTypeCode, Double amount, Date txnDate) {
    }
}

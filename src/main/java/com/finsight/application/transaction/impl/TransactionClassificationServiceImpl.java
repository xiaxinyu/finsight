package com.finsight.application.transaction.impl;

import com.alibaba.fastjson.JSON;
import com.finsight.application.consume.ClassificationService;
import com.finsight.application.transaction.ITransactionClassificationService;
import com.finsight.core.AppServiceException;
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

    @Override
    public Optional<String> classifyForApi(Transaction transaction, String bankCode, String cardTypeCode)
            throws AppServiceException {
        try {
            String narration = transaction.getTransactionDesc();
            bankCode = StringUtils.trimToEmpty(bankCode);
            cardTypeCode = StringUtils.trimToEmpty(cardTypeCode);
            if (!StringUtils.isNotBlank(bankCode)) {
                bankCode = StringUtils.trimToEmpty(transaction.getBankCardName());
            }
            if (!StringUtils.isNotBlank(cardTypeCode)) {
                cardTypeCode = StringUtils.trimToEmpty(transaction.getCardTypeName());
            }
            if (StringUtils.isNotBlank(cardTypeCode)) {
                cardTypeCode = cardTypeCode.trim().toLowerCase();
            }
            Double amount = transaction.getBalanceMoney();
            if (amount != null) {
                amount = Math.abs(amount);
            }
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
            List<ClassificationService.Result> rs = classificationService.classifyTopN(
                    narration, bankCode, cardTypeCode, amount, txnDate, 5);
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
            String narration = transaction.getTransactionDesc();
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
}

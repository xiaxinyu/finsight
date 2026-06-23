package com.finsight.application.transaction.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.finsight.application.transaction.ITransactionBatchUpdateService;
import com.finsight.application.transaction.ITransactionService;
import com.finsight.application.transaction.TransactionCategoryFieldNormalizer;
import com.finsight.common.exception.AppServiceException;
import com.finsight.domain.model.Transaction;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TransactionBatchUpdateServiceImpl implements ITransactionBatchUpdateService {

    private static final Logger log = LoggerFactory.getLogger(TransactionBatchUpdateServiceImpl.class);

    @Autowired
    private ITransactionService transactionService;

    @Autowired
    private TransactionCategoryFieldNormalizer categoryFieldNormalizer;

    @Override
    public Optional<String> batchUpdateTransactions(String payload, String userName) {
        List<Transaction> transactions = parseTransactions(payload);
        if (transactions == null || transactions.isEmpty()) {
            return Optional.empty();
        }
        int success = 0;
        int failed = 0;
        for (Transaction c : transactions) {
            try {
                String id = StringUtils.trimToEmpty(c.getId());
                String code = StringUtils.trimToEmpty(c.getConsumeCode());
                if (id.isEmpty() || code.isEmpty()) {
                    failed++;
                    continue;
                }
                categoryFieldNormalizer.normalize(c);
                transactionService.updateTransaction(c, userName);
                success++;
            } catch (AppServiceException e) {
                failed++;
                log.warn("batch update failed for id={}, error={}", c.getId(), e.getMessage());
            } catch (Exception e) {
                failed++;
                log.warn("batch update failed for id={}, error={}", c.getId(), e.getMessage());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        return Optional.of(JSONObject.toJSONString(result));
    }

    private static List<Transaction> parseTransactions(String payload) {
        try {
            return JSON.parseArray(payload, Transaction.class);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }
}

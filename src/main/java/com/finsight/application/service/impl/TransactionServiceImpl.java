package com.finsight.application.service.impl;

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
import com.finsight.application.service.ITransactionService;
import com.alibaba.fastjson.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

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

    @Override
    public String homeSummary(Integer year) throws AppServiceException {
        try {
            if (year == null) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                year = cal.get(java.util.Calendar.YEAR);
            }
            List<KeyValue> buckets = transactionMapper.homeSummaryExpenseBuckets(year);
            List<KeyValue> bucketsPrev = transactionMapper.homeSummaryExpenseBucketsPrev(year);
            double income = transactionMapper.sumIncomeByYear(year) == null ? 0.0 : transactionMapper.sumIncomeByYear(year);
            double debt = transactionMapper.sumDebtPaymentsByYear(year) == null ? 0.0 : transactionMapper.sumDebtPaymentsByYear(year);
            int refunds = transactionMapper.countRefundsByYear(year) == null ? 0 : transactionMapper.countRefundsByYear(year);

            java.util.Map<String, Double> bm = new java.util.LinkedHashMap<>();
            double expenseTotal = 0.0;
            if (buckets != null) {
                for (KeyValue kv : buckets) {
                    String k = kv.getKey();
                    double v = asDouble(kv.getValue());
                    bm.put(k, v);
                    expenseTotal += v;
                }
            }
            double life = bm.getOrDefault("life", 0.0);
            double fixed = bm.getOrDefault("fixed", 0.0);
            double shopping = bm.getOrDefault("shopping", 0.0);
            double entertainment = bm.getOrDefault("entertainment", 0.0);
            double investment = bm.getOrDefault("investment", 0.0);
            double other = bm.getOrDefault("other", 0.0);
            double edu = bm.getOrDefault("edu", 0.0);

            double div = income <= 0 ? 1.0 : income;
            java.util.Map<String, Double> ratios = new java.util.LinkedHashMap<>();
            ratios.put("life_vs_income", life / div);
            ratios.put("fixed_vs_income", fixed / div);
            ratios.put("shopping_vs_income", shopping / div);
            ratios.put("entertainment_vs_income", entertainment / div);
            ratios.put("investment_vs_income", investment / div);

            double savingsRate = income <= 0 ? 0.0 : Math.max(0, (income - expenseTotal) / income);
            double spendControl = Math.max(0, 100.0 - (expenseTotal > income ? 30.0 : 0.0) - (fixed / (expenseTotal <= 0 ? 1.0 : expenseTotal) > 0.35 ? 10.0 : 0.0));
            double savingsScore = Math.min(100.0, savingsRate * 100.0);
            double investAwareness = Math.min(100.0, (investment / (expenseTotal <= 0 ? 1.0 : expenseTotal)) * 120.0);
            double debtRisk = Math.max(0, 100.0 - Math.min(100.0, (debt / (income <= 0 ? 1.0 : income)) * 150.0));
            double rationality = Math.max(0, 100.0 - Math.min(40.0, refunds * 2.0));
            double growthTrend = 50.0;
            if (bucketsPrev != null && !bucketsPrev.isEmpty()) {
                java.util.Map<String, Double> prev = new java.util.HashMap<>();
                double prevTotal = 0.0;
                for (KeyValue kv : bucketsPrev) {
                    double v = asDouble(kv.getValue());
                    prev.put(kv.getKey(), v);
                    prevTotal += v;
                }
                double invCurrPct = investment / (expenseTotal <= 0 ? 1.0 : expenseTotal);
                double invPrevPct = prev.getOrDefault("investment", 0.0) / (prevTotal <= 0 ? 1.0 : prevTotal);
                double fixedCurrPct = fixed / (expenseTotal <= 0 ? 1.0 : expenseTotal);
                double fixedPrevPct = prev.getOrDefault("fixed", 0.0) / (prevTotal <= 0 ? 1.0 : prevTotal);
                growthTrend = 50.0 + (invCurrPct - invPrevPct) * 100.0 - (fixedCurrPct - fixedPrevPct) * 100.0;
                if (growthTrend < 0) growthTrend = 0;
                if (growthTrend > 100) growthTrend = 100;
            }

            double totalScore =
                    (spendControl * 0.30) +
                    (savingsScore * 0.20) +
                    (investAwareness * 0.15) +
                    (debtRisk * 0.15) +
                    (rationality * 0.10) +
                    (growthTrend * 0.10);

            java.util.Map<String, Object> json = new java.util.LinkedHashMap<>();
            json.put("year", year);
            java.util.Map<String, Double> bucketsOut = new java.util.LinkedHashMap<>();
            bucketsOut.put("life", roundPct(life, expenseTotal));
            bucketsOut.put("fixed", roundPct(fixed, expenseTotal));
            bucketsOut.put("shopping", roundPct(shopping, expenseTotal));
            bucketsOut.put("entertainment", roundPct(entertainment, expenseTotal));
            bucketsOut.put("investment", roundPct(investment, expenseTotal));
            bucketsOut.put("edu", roundPct(edu, expenseTotal));
            bucketsOut.put("other", roundPct(other, expenseTotal));
            json.put("buckets_pct", bucketsOut);
            json.put("income_total", income);
            json.put("expense_total", expenseTotal);
            json.put("ratios", ratios);
            java.util.Map<String, Object> score = new java.util.LinkedHashMap<>();
            score.put("total", Math.round(totalScore));
            java.util.Map<String, Object> dims = new java.util.LinkedHashMap<>();
            dims.put("spend_control", Math.round(spendControl));
            dims.put("savings_rate", Math.round(savingsScore));
            dims.put("invest_awareness", Math.round(investAwareness));
            dims.put("debt_risk", Math.round(debtRisk));
            java.util.Map<String, Object> dims2 = new java.util.LinkedHashMap<>();
            dims2.put("rationality", Math.round(rationality));
            dims2.put("growth_trend", Math.round(growthTrend));
            score.put("dimensions", dims);
            json.put("health_score", score);

            String summaryText = String.format("📌 %d Consumption Structure: Life %.0f%% · Fixed %.0f%% · Shopping %.0f%% · Entertainment %.0f%% · Investment %.0f%% · Education %.0f%% · Other %.0f%%",
                    year,
                    bucketsOut.get("life"),
                    bucketsOut.get("fixed"),
                    bucketsOut.get("shopping"),
                    bucketsOut.get("entertainment"),
                    bucketsOut.get("investment"),
                    bucketsOut.get("edu"),
                    bucketsOut.get("other"));
            json.put("summary_text", summaryText);
            return com.alibaba.fastjson.JSONObject.toJSONString(json);
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
    }

    private double roundPct(double part, double total){
        if (total <= 0) return 0.0;
        return Math.round((part / total) * 100.0);
    }

    private double asDouble(String s){
        if (s == null) return 0.0;
        try { return Double.parseDouble(s); } catch (Exception e){ return 0.0; }
    }
}

package com.finsight.application.transaction.impl;

import com.finsight.common.util.DateTool;
import com.finsight.common.util.StringTool;
import com.finsight.domain.model.Transaction;
import com.finsight.domain.model.KeyValue;
import com.finsight.domain.model.Page;
import com.finsight.application.card.BankCardService;
import com.finsight.domain.model.BankCard;
import com.finsight.common.exception.AppException;
import com.finsight.common.exception.AppServiceException;
import com.finsight.application.analytics.MetricRefreshTrigger;
import com.finsight.application.transaction.ITransactionService;
import com.finsight.application.transaction.TransactionAmountNormalizer;
import com.finsight.application.transaction.TransactionCategoryFieldNormalizer;
import com.finsight.application.transaction.TransactionCategoryFieldSync;
import com.finsight.application.transaction.TransactionFieldSanitizer;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.application.query.TransactionQuery;
import com.alibaba.fastjson.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service("transactionService")
public class TransactionServiceImpl implements ITransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);
    private static final long HOME_SUMMARY_CACHE_TTL_MS = 120_000L;
    private final Map<Integer, CacheEntry> homeSummaryCache = new ConcurrentHashMap<>();
    @Autowired
    BankCardService bankCardService;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    MetricRefreshTrigger metricRefreshTrigger;

    @Autowired
    TransactionCategoryFieldNormalizer categoryFieldNormalizer;

    @Override
    public void updateTransaction(Transaction transaction, String userName) throws AppServiceException {
        try {
            if (transaction == null || StringUtils.isBlank(transaction.getId())) {
                throw new AppServiceException("Transaction id is required");
            }
            Transaction existing = transactionRepository.selectById(transaction.getId());
            if (existing == null) {
                throw new AppServiceException("Transaction not found");
            }
            if ("transfer".equalsIgnoreCase(existing.getTxnKind())) {
                throw new AppServiceException("Transfer transactions cannot be edited inline. Undo the transfer first.");
            }
            if (transaction.getTransactionDate() != null) {
                transaction.setBookKeepingDate(transaction.getTransactionDate());
            }
            String kind = StringUtils.isNotBlank(transaction.getTxnKind())
                    ? transaction.getTxnKind()
                    : inferTxnKind(existing);
            if (transaction.getTxnKind() != null
                    || transaction.getBalanceMoney() != null
                    || transaction.getIncomeMoney() != null) {
                normalizeAmounts(transaction, kind);
            }
            if (transaction.hasCategoryFieldPatch()) {
                categoryFieldNormalizer.normalize(transaction);
            }
            transaction.setUpdateUser(userName);
            transactionRepository.updateTransaction(transaction);
            invalidateHomeSummaryCache();
            List<Date> dates = new ArrayList<>();
            if (existing.getTransactionDate() != null) {
                dates.add(existing.getTransactionDate());
            }
            if (transaction.getTransactionDate() != null) {
                dates.add(transaction.getTransactionDate());
            }
            metricRefreshTrigger.afterTransactionsChanged(dates, userName);
        } catch (AppServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
    }

    private static String inferTxnKind(Transaction t) {
        if (StringUtils.isNotBlank(t.getTxnKind())) {
            return t.getTxnKind();
        }
        if (t.getIncomeMoney() != null && t.getIncomeMoney() > 0) {
            return "income";
        }
        if (t.getBalanceMoney() != null && t.getBalanceMoney() < 0) {
            return "income";
        }
        return "expense";
    }

    private static void normalizeAmounts(Transaction t, String kind) {
        if ("income".equalsIgnoreCase(kind)) {
            double amt = 0;
            if (t.getIncomeMoney() != null && t.getIncomeMoney() != 0) {
                amt = Math.abs(t.getIncomeMoney());
            } else if (t.getBalanceMoney() != null && t.getBalanceMoney() != 0) {
                amt = Math.abs(t.getBalanceMoney());
            }
            if (amt > 0) {
                t.setIncomeMoney(amt);
                t.setBalanceMoney(0.0);
            }
            t.setTxnKind("income");
        } else if ("expense".equalsIgnoreCase(kind)) {
            double amt = 0;
            if (t.getBalanceMoney() != null && t.getBalanceMoney() != 0) {
                amt = Math.abs(t.getBalanceMoney());
            } else if (t.getIncomeMoney() != null && t.getIncomeMoney() != 0) {
                amt = Math.abs(t.getIncomeMoney());
            }
            if (amt > 0) {
                t.setBalanceMoney(amt);
                t.setIncomeMoney(0.0);
            }
            t.setTxnKind("expense");
        }
    }

    @Override
    public int incomeToExpense(List<String> ids, String userName) throws AppServiceException {
        try {
            if (ids == null || ids.isEmpty()) {
                return 0;
            }
            return transactionRepository.incomeToExpense(ids, userName);
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
    }

    @Override
    public int expenseToIncome(List<String> ids, String userName) throws AppServiceException {
        try {
            if (ids == null || ids.isEmpty()) {
                return 0;
            }
            return transactionRepository.expenseToIncome(ids, userName);
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
    }

    @Override
    public void deleteTransaction(String id) throws AppServiceException {
        try {
            transactionRepository.deleteTransaction(id);
            invalidateHomeSummaryCache();
            metricRefreshTrigger.afterTransactionsChanged(List.of(), null);
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
    }

    @Override
    public List<Transaction> getTransactions(Transaction transaction, Page page) throws AppServiceException {
        List<Transaction> result = null;
        try {
            log.info("Query transactions：page={}", page);
            result = transactionRepository.getTransactions(toQuery(transaction), page);
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
        return result;
    }

    @Override
    public int countTransaction(Transaction transaction) throws AppServiceException {
        int result = 0;
        try {
            result = transactionRepository.countTransaction(toQuery(transaction));
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
        return result;
    }

    @Override
    public void deleteByStatementId(String statementId) {
        transactionRepository.deleteByStatementId(statementId);
    }

    @Override
    public void addTransactions(List<String[]> rowDatas, String userName, String recordID) {
        if (CollectionUtils.isEmpty(rowDatas)) {
            throw new AppException("No exist original transaction data, can't call add transactions!");
        }

        Map<String, BankCard> cardMap = new java.util.HashMap<>();
        for (BankCard card : bankCardService.listAllEnabled()) {
            if (card != null && card.getId() != null) {
                cardMap.put(card.getId(), card);
            }
        }

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
                String cardId = StringTool.cleanStr(rowData[0]);
                transaction.setCardId(cardId);
                transaction.setTransactionDate(DateTool.changeStringToDate(StringTool.cleanStr(rowData[1]), DateTool.DF_YYYY_MM_DD));
                transaction.setBookKeepingDate(DateTool.changeStringToDate(StringTool.cleanStr(rowData[2]), DateTool.DF_YYYY_MM_DD));
                transaction.setTransactionDesc(StringTool.cleanStr(rowData[3]));
                transaction.setBalanceCurrency(StringTool.cleanStr(rowData[4]));

                String balanceMoney = StringTool.cleanStr(rowData[5]);
                transaction.setBalanceMoney(StringUtils.isBlank(balanceMoney) ? 0 : Double.parseDouble(balanceMoney));

                transaction.setCardTypeId(1);
                BankCard card = cardMap.get(cardId);
                transaction.setCardTypeName(card != null ? card.getCardName() : "Unknown Card");
                transaction.setRecordID(recordID);
                transactionRepository.insert(transaction);
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
                prepareForInsert(transaction, userName);
                transactionRepository.insert(transaction);
                success++;
            } catch (Exception e) {
                log.error("Saving transaction has error: transaction={}", transaction, e);
            }
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importTransactionsStrict(List<Transaction> transactions, String userName) throws AppServiceException {
        if (transactions == null || transactions.isEmpty()) {
            throw new AppServiceException("No transactions to import");
        }
        try {
            int success = 0;
            List<Date> dates = new ArrayList<>();
            for (Transaction transaction : transactions) {
                prepareForInsert(transaction, userName);
                transactionRepository.insert(transaction);
                if (transaction.getTransactionDate() != null) {
                    dates.add(transaction.getTransactionDate());
                }
                success++;
            }
            invalidateHomeSummaryCache();
            metricRefreshTrigger.afterTransactionsChanged(dates, userName);
            return success;
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
    }

    @Override
    public void invalidateHomeSummaryCache() {
        homeSummaryCache.clear();
    }

    private void prepareForInsert(Transaction transaction, String userName) {
        TransactionAmountNormalizer.normalize(transaction);
        TransactionFieldSanitizer.sanitize(transaction);
        if (StringUtils.isNotBlank(TransactionCategoryFieldSync.resolveCanonicalCode(transaction))) {
            categoryFieldNormalizer.normalize(transaction);
        }
        if (transaction.getId() == null || transaction.getId().trim().isEmpty()
                || transactionRepository.selectById(transaction.getId()) != null) {
            transaction.setId(StringTool.generateID());
        }
        transaction.setCreateUser(userName);
        transaction.setUpdateUser(userName);
    }

    private void fetchTransactionParam(Transaction transaction) {
        if (StringTool.isNullOrEmpty(transaction.getCardTypeName())) {
            transaction.setCardTypeName(null);
        }
        if (StringTool.isNullOrEmpty(transaction.getDemoArea())) {
            transaction.setDemoArea(null);
        }
    }

    private static TransactionQuery toQuery(Transaction t) {
        TransactionQuery q = new TransactionQuery();
        if (t == null) {
            return q;
        }
        q.setConsumptionType(t.getConsumptionType());
        q.setBankCardId(t.getBankCardId());
        q.setCardTypeName(t.getCardTypeName());
        q.setConsumeID(t.getConsumeID());
        q.setConsumeCode(t.getConsumeCode());
        q.setConsumeName(t.getConsumeName());
        q.setDemoArea(t.getDemoArea());
        return q;
    }

    @Override
    public String consumeReport(Transaction transaction) throws AppServiceException {
        String result = StringTool.EMPTY;
        try {
            fetchTransactionParam(transaction);
            List<com.finsight.domain.model.CategoryAggregate> list = transactionRepository.consumeReport(toQuery(transaction));
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
            List<KeyValue> list = transactionRepository.weekConsumeReport(toQuery(transaction));
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
            List<KeyValue> list = transactionRepository.monthConsumeReport(toQuery(transaction));
            result = JSONArray.toJSONString(list).toString();
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
        return result;
    }

    @Override
    public String monthIncomeReport(Transaction transaction) throws AppServiceException {
        String result = StringTool.EMPTY;
        try {
            fetchTransactionParam(transaction);
            List<KeyValue> list = transactionRepository.monthIncomeReport(toQuery(transaction));
            result = JSONArray.toJSONString(list).toString();
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
        return result;
    }

    @Override
    public String monthExpenseReport(Transaction transaction) throws AppServiceException {
        String result = StringTool.EMPTY;
        try {
            fetchTransactionParam(transaction);
            List<KeyValue> list = transactionRepository.monthExpenseReport(toQuery(transaction));
            result = JSONArray.toJSONString(list).toString();
        } catch (Exception e) {
            throw new AppServiceException(e);
        }
        return result;
    }

    @Override
    public String homeSummary(Integer year) throws AppServiceException {
        return homeSummary(year, null, null);
    }

    @Override
    public String homeSummary(Integer year, java.util.Date rangeStart, java.util.Date rangeEnd)
            throws AppServiceException {
        try {
            if (year == null) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                year = cal.get(java.util.Calendar.YEAR);
            }
            boolean ranged = rangeStart != null && rangeEnd != null;
            if (!ranged) {
                CacheEntry cached = homeSummaryCache.get(year);
                long now = System.currentTimeMillis();
                if (cached != null && (now - cached.getCreatedAt()) <= HOME_SUMMARY_CACHE_TTL_MS) {
                    return cached.getPayload();
                }
            }

            List<KeyValue> buckets = ranged
                    ? transactionRepository.homeSummaryExpenseBucketsForRange(rangeStart, rangeEnd)
                    : transactionRepository.homeSummaryExpenseBuckets(year);
            List<KeyValue> bucketsPrev = ranged ? java.util.Collections.emptyList()
                    : transactionRepository.homeSummaryExpenseBucketsPrev(year);
            Double incomeResult = ranged
                    ? transactionRepository.sumIncomeForRange(rangeStart, rangeEnd)
                    : transactionRepository.sumIncomeByYear(year);
            Double debtResult = transactionRepository.sumDebtPaymentsByYear(year);
            Integer refundsResult = transactionRepository.countRefundsByYear(year);
            double income = incomeResult == null ? 0.0 : incomeResult;
            double debt = debtResult == null ? 0.0 : debtResult;
            int refunds = refundsResult == null ? 0 : refundsResult;

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
            dims.putAll(dims2);
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
            String payload = com.alibaba.fastjson.JSONObject.toJSONString(json);
            if (!ranged) {
                long now = System.currentTimeMillis();
                homeSummaryCache.put(year, new CacheEntry(now, payload));
            }
            return payload;
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

    private static class CacheEntry {
        private final long createdAt;
        private final String payload;

        private CacheEntry(long createdAt, String payload) {
            this.createdAt = createdAt;
            this.payload = payload;
        }

        private long getCreatedAt() {
            return createdAt;
        }

        private String getPayload() {
            return payload;
        }
    }
}

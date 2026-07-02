package com.finsight.application.finance;

import com.finsight.application.authentication.LedgerUserScope;
import com.finsight.domain.model.KeyValue;
import com.finsight.infrastructure.mapper.FinancialMapper;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Ledger queries scoped to the authenticated user (admin does not bypass isolation).
 */
@Component
public class UserScopedFinancialQueries {

    private final FinancialMapper financialMapper;
    private final LedgerUserScope ledgerUserScope;

    public UserScopedFinancialQueries(FinancialMapper financialMapper, LedgerUserScope ledgerUserScope) {
        this.financialMapper = financialMapper;
        this.ledgerUserScope = ledgerUserScope;
    }

    private String owner() {
        return ledgerUserScope.resolve();
    }

    public int countUnclassified() {
        return financialMapper.countUnclassified(owner());
    }

    public int countTransferGroups() {
        return financialMapper.countTransferGroups(owner());
    }

    public List<Map<String, Object>> listTransferGroups() {
        return financialMapper.listTransferGroups(owner());
    }

    public List<KeyValue> latestBalancesFromBankCards() {
        return financialMapper.latestBalancesFromBankCards(owner());
    }

    public double sumCurrentLiabilities() {
        Double v = financialMapper.sumCurrentLiabilities(owner());
        return v == null ? 0.0 : v;
    }

    public Double sumExpenseSince(Date since) {
        return financialMapper.sumExpenseSince(since, owner());
    }

    public Double sumIncomeSince(Date since) {
        return financialMapper.sumIncomeSince(since, owner());
    }

    public Double sumFixedBucketYear(int year) {
        return financialMapper.sumFixedBucketYear(year, owner());
    }

    public Double sumExpenseByBucketSince(Date since, String bucketKey) {
        return financialMapper.sumExpenseByBucketSince(since, bucketKey, owner());
    }

    public Double sumExpenseByCategorySince(Date since, String categoryCode) {
        return financialMapper.sumExpenseByCategorySince(since, categoryCode, owner());
    }

    public Double sumExpenseBetween(Date start, Date end) {
        return financialMapper.sumExpenseBetween(start, end, owner());
    }

    public Double sumExpenseByBucketBetween(Date start, Date end, String bucketKey) {
        return financialMapper.sumExpenseByBucketBetween(start, end, bucketKey, owner());
    }

    public Double sumExpenseByCategoryBetween(Date start, Date end, String categoryCode) {
        return financialMapper.sumExpenseByCategoryBetween(start, end, categoryCode, owner());
    }
}

package com.finsight.application.classification;

import java.util.List;

/**
 * Shared transaction/rule stats SQL for category asset and impact preview.
 */
public final class CategoryStatsSupport {

    private CategoryStatsSupport() {
    }

    public static String countTransactionsSql(List<String> refs) {
        return "select count(*) from `transaction` t where coalesce(t.deleted,0)=0 and "
                + CategoryImpactSupport.transactionMatchSql(refs);
    }

    public static String sumTransactionAmountSql(List<String> refs) {
        return "select coalesce(round(sum("
                + "abs(coalesce(t.expense_amount,0)) + abs(coalesce(t.income_money,0))"
                + "), 2), 0) from `transaction` t where coalesce(t.deleted,0)=0 and "
                + CategoryImpactSupport.transactionMatchSql(refs);
    }

    public static String lastTransactionDateSql(List<String> refs) {
        return "select max(t.transaction_date) from `transaction` t where coalesce(t.deleted,0)=0 and "
                + CategoryImpactSupport.transactionMatchSql(refs);
    }
}

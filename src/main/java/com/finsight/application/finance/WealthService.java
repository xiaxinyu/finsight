package com.finsight.application.finance;

import com.finsight.domain.model.KeyValue;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WealthService {

    private final FinancialAccountService accountService;
    private final UserScopedFinancialQueries scopedFinancialQueries;
    private final CashflowService cashflowService;

    public WealthService(FinancialAccountService accountService,
                         UserScopedFinancialQueries scopedFinancialQueries,
                         CashflowService cashflowService) {
        this.accountService = accountService;
        this.scopedFinancialQueries = scopedFinancialQueries;
        this.cashflowService = cashflowService;
    }

    public Map<String, Object> snapshot() {
        List<KeyValue> accounts = accountService.latestBalances();
        double assets = 0;
        double liabilities = 0;
        for (KeyValue kv : accounts) {
            double v = parseAmount(kv.getValue());
            String name = kv.getKey() == null ? "" : kv.getKey().toLowerCase();
            if (name.contains("credit") || v < 0) {
                liabilities += Math.abs(v);
            } else {
                assets += v;
            }
        }

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        double incomeYtd = safe(scopedFinancialQueries.sumIncomeSince(yearStart(year)));
        double expenseYtd = safe(scopedFinancialQueries.sumExpenseSince(yearStart(year)));
        double savingsRate = incomeYtd > 0 ? (incomeYtd - expenseYtd) / incomeYtd : 0;

        Map<String, Object> health = new LinkedHashMap<>();
        Map<String, Object> cashflow = cashflowService.metrics();
        double runway = ((Number) cashflow.get("runwayMonths")).doubleValue();
        health.put("liquidity", Math.min(100, runway * 16.67));
        health.put("savingsRate", Math.min(100, savingsRate * 100));
        health.put("fixedBurden", incomeYtd > 0 ? Math.min(100, safe(scopedFinancialQueries.sumFixedBucketYear(year)) / incomeYtd * 100) : 0);
        health.put("debtPressure", liabilities > 0 && assets > 0 ? Math.min(100, liabilities / assets * 100) : 0);
        health.put("emergencyMonths", runway);
        health.put("total", average(health));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("netWorth", assets - liabilities);
        out.put("assets", assets);
        out.put("liabilities", liabilities);
        out.put("accounts", accounts);
        out.put("savingsRate", savingsRate);
        out.put("healthScore", health);
        out.put("cashflow", cashflow);
        return out;
    }

    private static double average(Map<String, Object> dims) {
        double sum = 0;
        int n = 0;
        for (Map.Entry<String, Object> e : dims.entrySet()) {
            if ("total".equals(e.getKey()) || "emergencyMonths".equals(e.getKey())) {
                continue;
            }
            sum += ((Number) e.getValue()).doubleValue();
            n++;
        }
        return n == 0 ? 0 : sum / n;
    }

    private static java.util.Date yearStart(int year) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, 0);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static double safe(Double v) {
        return v == null ? 0 : v;
    }

    private static double parseAmount(String v) {
        if (v == null || v.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

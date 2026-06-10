package com.finsight.application.finance;

import com.finsight.domain.model.Budget;
import com.finsight.domain.model.BudgetLine;
import com.finsight.infrastructure.mapper.FinancialMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BudgetService {

    private final PlanningPreferencesStore preferencesStore;
    private final FinancialMapper financialMapper;

    public BudgetService(PlanningPreferencesStore preferencesStore, FinancialMapper financialMapper) {
        this.preferencesStore = preferencesStore;
        this.financialMapper = financialMapper;
    }

    public Budget currentMonthlyBudget() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        Budget b = new Budget();
        b.setId("monthly-" + year + "-" + month);
        b.setName(year + "-" + month + " Budget");
        b.setPeriodType("monthly");
        b.setYear(year);
        b.setMonth(month);
        b.setDeleted(0);
        return b;
    }

    public List<BudgetLine> linesForBudget(String budgetId) {
        return preferencesStore.budgetLinesForCurrentMonth();
    }

    public BudgetLine saveLine(BudgetLine line) {
        if (line.getBudgetId() == null || line.getBudgetId().isBlank()) {
            line.setBudgetId(currentMonthlyBudget().getId());
        }
        return preferencesStore.upsertBudgetLine(line);
    }

    public List<Map<String, Object>> budgetVsActual() {
        Budget budget = currentMonthlyBudget();
        List<BudgetLine> lines = linesForBudget(budget.getId());
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date monthStart = cal.getTime();
        double actualTotal = safe(financialMapper.sumExpenseSince(monthStart));

        List<Map<String, Object>> rows = new ArrayList<>();
        double limitTotal = 0;
        for (BudgetLine line : lines) {
            Map<String, Object> row = new HashMap<>();
            row.put("bucketKey", line.getBucketKey());
            row.put("categoryCode", line.getCategoryCode());
            row.put("limit", line.getLimitAmount());
            double actual = resolveActual(monthStart, line);
            row.put("actual", actual);
            double limit = line.getLimitAmount() == null ? 0 : line.getLimitAmount().doubleValue();
            row.put("remaining", limit - actual);
            limitTotal += limit;
            rows.add(row);
        }
        if (rows.isEmpty()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("bucketKey", "all");
            row.put("limit", BigDecimal.ZERO);
            row.put("actual", actualTotal);
            row.put("remaining", -actualTotal);
            rows.add(row);
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("budgetId", budget.getId());
        meta.put("limitTotal", limitTotal);
        meta.put("actualTotal", actualTotal);
        meta.put("lines", rows);
        return List.of(meta);
    }

    private double resolveActual(Date monthStart, BudgetLine line) {
        if (line.getCategoryCode() != null && !line.getCategoryCode().isBlank()) {
            return safe(financialMapper.sumExpenseByCategorySince(monthStart, line.getCategoryCode()));
        }
        String bucket = line.getBucketKey() == null || line.getBucketKey().isBlank() ? "all" : line.getBucketKey();
        return safe(financialMapper.sumExpenseByBucketSince(monthStart, bucket));
    }

    private static double safe(Double v) {
        return v == null ? 0 : v;
    }
}

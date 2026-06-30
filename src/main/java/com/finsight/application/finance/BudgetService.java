package com.finsight.application.finance;

import com.finsight.application.analytics.AnalyticsDateRange;
import com.finsight.application.analytics.FinanceSemanticMetricsRepository;
import com.finsight.application.analytics.MetricRefreshTrigger;
import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.classification.BudgetSemanticBuckets;
import com.finsight.application.support.ListingDateSupport;
import com.finsight.common.exception.AppServiceException;
import com.finsight.domain.model.Budget;
import com.finsight.domain.model.BudgetLine;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BudgetService {

    private final PlanningPreferencesGateway planningGateway;
    private final FinanceSemanticMetricsRepository semanticMetricsRepository;
    private final MetricRefreshTrigger metricRefreshTrigger;
    private final AuthenticationFacade authenticationFacade;

    public BudgetService(PlanningPreferencesGateway planningGateway,
                         FinanceSemanticMetricsRepository semanticMetricsRepository,
                         MetricRefreshTrigger metricRefreshTrigger,
                         AuthenticationFacade authenticationFacade) {
        this.planningGateway = planningGateway;
        this.semanticMetricsRepository = semanticMetricsRepository;
        this.metricRefreshTrigger = metricRefreshTrigger;
        this.authenticationFacade = authenticationFacade;
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
        return planningGateway.budgetLinesForCurrentMonth();
    }

    public BudgetLine saveLine(BudgetLine line) {
        if (line.getBudgetId() == null || line.getBudgetId().isBlank()) {
            line.setBudgetId(currentMonthlyBudget().getId());
        }
        BudgetLine saved = planningGateway.upsertBudgetLine(line);
        metricRefreshTrigger.refreshCurrentMonth();
        return saved;
    }

    public List<Map<String, Object>> budgetVsActual() throws AppServiceException {
        return budgetVsActual(null, null);
    }

    public List<Map<String, Object>> budgetVsActual(String startStr, String endStr) throws AppServiceException {
        Budget budget = currentMonthlyBudget();
        List<BudgetLine> lines = linesForBudget(budget.getId());
        LocalDate rangeStart;
        LocalDate rangeEnd;
        if (StringUtils.isNotBlank(startStr) && StringUtils.isNotBlank(endStr)) {
            Date[] range = ListingDateSupport.parseMmDdYyyyOrDefaultOneYear(startStr, endStr);
            rangeStart = AnalyticsDateRange.toLocalDate(range[0]);
            rangeEnd = AnalyticsDateRange.toLocalDate(range[1]);
        } else {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            rangeStart = AnalyticsDateRange.toLocalDate(cal.getTime());
            rangeEnd = LocalDate.now();
        }
        String userId = userKey();
        double actualTotal = semanticMetricsRepository.sumConsumptionExpense(userId, rangeStart, rangeEnd);

        List<Map<String, Object>> rows = new ArrayList<>();
        double limitTotal = 0;
        for (BudgetLine line : lines) {
            Map<String, Object> row = new LinkedHashMap<>();
            String bucket = line.getBucketKey() == null || line.getBucketKey().isBlank()
                    ? "all" : line.getBucketKey();
            row.put("bucketKey", bucket);
            row.put("categoryCode", line.getCategoryCode());
            row.put("classification", line.getCategoryCode() != null && !line.getCategoryCode().isBlank()
                    ? BudgetSemanticBuckets.displayLabel(line.getCategoryCode())
                    : BudgetSemanticBuckets.displayLabel(bucket));
            row.put("limit", line.getLimitAmount());
            double actual = resolveActual(userId, rangeStart, rangeEnd, line);
            row.put("actual", actual);
            double limit = line.getLimitAmount() == null ? 0 : line.getLimitAmount().doubleValue();
            row.put("remaining", limit - actual);
            limitTotal += limit;
            rows.add(row);
        }
        if (rows.isEmpty()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("bucketKey", "all");
            row.put("classification", BudgetSemanticBuckets.displayLabel("all"));
            row.put("limit", BigDecimal.ZERO);
            row.put("actual", actualTotal);
            row.put("remaining", -actualTotal);
            rows.add(row);
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("budgetId", budget.getId());
        meta.put("limitTotal", limitTotal);
        meta.put("actualTotal", actualTotal);
        meta.put("periodStart", rangeStart);
        meta.put("periodEnd", rangeEnd);
        meta.put("metricsSource", "v_transaction_finance_semantics.semantic_tag");
        meta.put("lines", rows);
        return List.of(meta);
    }

    private double resolveActual(String userId, LocalDate rangeStart, LocalDate rangeEnd, BudgetLine line) {
        if (line.getCategoryCode() != null && !line.getCategoryCode().isBlank()) {
            return semanticMetricsRepository.sumBudgetActual(
                    userId, rangeStart, rangeEnd, line.getCategoryCode(), null);
        }
        String bucket = line.getBucketKey() == null || line.getBucketKey().isBlank() ? "all" : line.getBucketKey();
        return semanticMetricsRepository.sumBudgetActual(userId, rangeStart, rangeEnd, null, bucket);
    }

    private String userKey() {
        String user = authenticationFacade.getUserName();
        return user == null || user.isBlank() ? "_anonymous" : user.trim().toLowerCase(Locale.ROOT);
    }
}

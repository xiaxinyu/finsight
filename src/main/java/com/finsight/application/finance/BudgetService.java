package com.finsight.application.finance;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.domain.model.Budget;
import com.finsight.domain.model.BudgetLine;
import com.finsight.infrastructure.mapper.BudgetLineMapper;
import com.finsight.infrastructure.mapper.BudgetMapper;
import com.finsight.infrastructure.mapper.FinancialMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BudgetService {

    private final BudgetMapper budgetMapper;
    private final BudgetLineMapper budgetLineMapper;
    private final FinancialMapper financialMapper;
    private final AuthenticationFacade authenticationFacade;

    public BudgetService(BudgetMapper budgetMapper,
                         BudgetLineMapper budgetLineMapper,
                         FinancialMapper financialMapper,
                         AuthenticationFacade authenticationFacade) {
        this.budgetMapper = budgetMapper;
        this.budgetLineMapper = budgetLineMapper;
        this.financialMapper = financialMapper;
        this.authenticationFacade = authenticationFacade;
    }

    public Budget currentMonthlyBudget() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        Budget b = budgetMapper.selectOne(Wrappers.<Budget>lambdaQuery()
                .eq(Budget::getPeriodType, "monthly")
                .eq(Budget::getYear, year)
                .eq(Budget::getMonth, month)
                .eq(Budget::getDeleted, 0));
        if (b != null) {
            return b;
        }
        Budget created = new Budget();
        created.setId(UUID.randomUUID().toString());
        created.setName(year + "-" + month + " Budget");
        created.setPeriodType("monthly");
        created.setYear(year);
        created.setMonth(month);
        created.setDeleted(0);
        created.setCreateUser(authenticationFacade.getUserName());
        created.setCreateTime(new Date());
        budgetMapper.insert(created);
        return created;
    }

    public List<BudgetLine> linesForBudget(String budgetId) {
        return budgetLineMapper.selectList(Wrappers.<BudgetLine>lambdaQuery().eq(BudgetLine::getBudgetId, budgetId));
    }

    @Transactional
    public BudgetLine saveLine(BudgetLine line) {
        if (line.getId() == null || line.getId().isBlank()) {
            line.setId(UUID.randomUUID().toString());
            budgetLineMapper.insert(line);
        } else {
            budgetLineMapper.updateById(line);
        }
        return line;
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
            limitTotal += line.getLimitAmount() == null ? 0 : line.getLimitAmount().doubleValue();
            rows.add(row);
        }
        if (rows.isEmpty()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("bucketKey", "all");
            row.put("limit", BigDecimal.ZERO);
            row.put("actual", actualTotal);
            row.put("remaining", -actualTotal);
            rows.add(row);
        } else {
            for (Map<String, Object> row : rows) {
                row.put("actual", actualTotal / rows.size());
                double limit = ((BigDecimal) row.get("limit")).doubleValue();
                row.put("remaining", limit - ((Number) row.get("actual")).doubleValue());
            }
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("budgetId", budget.getId());
        meta.put("limitTotal", limitTotal);
        meta.put("actualTotal", actualTotal);
        meta.put("lines", rows);
        return List.of(meta);
    }

    private static double safe(Double v) {
        return v == null ? 0 : v;
    }
}

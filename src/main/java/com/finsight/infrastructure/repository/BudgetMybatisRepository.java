package com.finsight.infrastructure.repository;

import com.finsight.domain.model.Budget;
import com.finsight.domain.model.BudgetLine;
import com.finsight.domain.port.BudgetRepository;
import com.finsight.infrastructure.mapper.FinPlanningMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class BudgetMybatisRepository implements BudgetRepository {

    private final FinPlanningMapper mapper;

    public BudgetMybatisRepository(FinPlanningMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Budget findByUserAndMonth(String userId, String monthKey) {
        return mapper.findBudget(userId, monthKey);
    }

    @Override
    public Budget saveBudget(Budget budget, String userId) {
        String monthKey = monthKeyFrom(budget);
        if (budget.getId() == null || budget.getId().isBlank()) {
            budget.setId(UUID.randomUUID().toString());
        }
        if (mapper.findBudget(userId, monthKey) == null) {
            mapper.insertBudget(budget, userId, monthKey);
        } else {
            mapper.updateBudget(budget);
        }
        return budget;
    }

    @Override
    public List<BudgetLine> linesForBudget(String budgetId) {
        return mapper.listBudgetLines(budgetId);
    }

    @Override
    public BudgetLine upsertLine(BudgetLine line, String userId) {
        if (line.getId() == null || line.getId().isBlank()) {
            line.setId(UUID.randomUUID().toString());
            mapper.insertBudgetLine(line, userId);
        } else {
            mapper.updateBudgetLine(line);
        }
        return line;
    }

    @Override
    public boolean hasAnyForUser(String userId) {
        return mapper.countBudgetLinesForUser(userId) > 0;
    }

    private static String monthKeyFrom(Budget budget) {
        return budget.getYear() + "-" + budget.getMonth();
    }
}

package com.finsight.domain.port;

import com.finsight.domain.model.Budget;
import com.finsight.domain.model.BudgetLine;

import java.util.List;

public interface BudgetRepository {

    Budget findByUserAndMonth(String userId, String monthKey);

    Budget saveBudget(Budget budget, String userId);

    List<BudgetLine> linesForBudget(String budgetId);

    BudgetLine upsertLine(BudgetLine line, String userId);

    boolean hasAnyForUser(String userId);
}

package com.finsight.domain.port;

import com.finsight.domain.model.FinancialGoal;

import java.util.List;

public interface GoalRepository {

    List<FinancialGoal> listActive(String userId);

    FinancialGoal save(FinancialGoal goal, String userId);

    boolean hasAnyForUser(String userId);
}

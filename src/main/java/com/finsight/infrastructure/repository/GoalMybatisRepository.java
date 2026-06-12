package com.finsight.infrastructure.repository;

import com.finsight.domain.model.FinancialGoal;
import com.finsight.domain.port.GoalRepository;
import com.finsight.infrastructure.mapper.FinPlanningMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class GoalMybatisRepository implements GoalRepository {

    private final FinPlanningMapper mapper;

    public GoalMybatisRepository(FinPlanningMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<FinancialGoal> listActive(String userId) {
        return mapper.listGoals(userId);
    }

    @Override
    public FinancialGoal save(FinancialGoal goal, String userId) {
        if (goal.getId() == null || goal.getId().isBlank()) {
            goal.setId(UUID.randomUUID().toString());
            if (goal.getDeleted() == null) {
                goal.setDeleted(0);
            }
            mapper.insertGoal(goal, userId);
        } else {
            mapper.updateGoal(goal);
        }
        return goal;
    }

    @Override
    public boolean hasAnyForUser(String userId) {
        return mapper.countGoalsForUser(userId) > 0;
    }
}

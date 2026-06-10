package com.finsight.application.finance;

import com.finsight.domain.model.FinancialGoal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinancialGoalService {

    private final PlanningPreferencesStore preferencesStore;

    public FinancialGoalService(PlanningPreferencesStore preferencesStore) {
        this.preferencesStore = preferencesStore;
    }

    public List<FinancialGoal> list() {
        return preferencesStore.goals();
    }

    public FinancialGoal save(FinancialGoal goal) {
        if (goal.getCurrentAmount() == null) {
            goal.setCurrentAmount(BigDecimal.ZERO);
        }
        return preferencesStore.saveGoal(goal);
    }

    public Map<String, Object> progress(FinancialGoal goal) {
        BigDecimal target = goal.getTargetAmount() == null ? BigDecimal.ZERO : goal.getTargetAmount();
        BigDecimal current = goal.getCurrentAmount() == null ? BigDecimal.ZERO : goal.getCurrentAmount();
        BigDecimal remaining = target.subtract(current).max(BigDecimal.ZERO);
        BigDecimal pct = target.signum() == 0 ? BigDecimal.ZERO
                : current.multiply(BigDecimal.valueOf(100)).divide(target, 1, RoundingMode.HALF_UP);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("goal", goal);
        m.put("percent", pct);
        m.put("remaining", remaining);
        if (goal.getMonthlyContribution() != null && goal.getMonthlyContribution().signum() > 0) {
            int months = remaining.divide(goal.getMonthlyContribution(), 0, RoundingMode.CEILING).intValue();
            m.put("monthsToTarget", months);
        }
        return m;
    }
}

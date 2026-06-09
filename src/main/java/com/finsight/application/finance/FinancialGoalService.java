package com.finsight.application.finance;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.domain.model.FinancialGoal;
import com.finsight.infrastructure.mapper.FinancialGoalMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FinancialGoalService {

    private final FinancialGoalMapper goalMapper;
    private final AuthenticationFacade authenticationFacade;

    public FinancialGoalService(FinancialGoalMapper goalMapper, AuthenticationFacade authenticationFacade) {
        this.goalMapper = goalMapper;
        this.authenticationFacade = authenticationFacade;
    }

    public List<FinancialGoal> list() {
        return goalMapper.selectList(Wrappers.<FinancialGoal>lambdaQuery()
                .eq(FinancialGoal::getDeleted, 0)
                .orderByAsc(FinancialGoal::getTargetDate));
    }

    public FinancialGoal save(FinancialGoal goal) {
        if (goal.getId() == null || goal.getId().isBlank()) {
            goal.setId(UUID.randomUUID().toString());
            goal.setDeleted(0);
            goal.setCreateUser(authenticationFacade.getUserName());
            goal.setCreateTime(new Date());
            if (goal.getCurrentAmount() == null) {
                goal.setCurrentAmount(BigDecimal.ZERO);
            }
            goalMapper.insert(goal);
        } else {
            goal.setUpdateUser(authenticationFacade.getUserName());
            goal.setUpdateTime(new Date());
            goalMapper.updateById(goal);
        }
        return goal;
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

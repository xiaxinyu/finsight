package com.finsight.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.finsight.domain.model.Bill;
import com.finsight.domain.model.Budget;
import com.finsight.domain.model.BudgetLine;
import com.finsight.domain.model.FinancialGoal;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface FinPlanningMapper {

    Budget findBudget(@Param("userId") String userId, @Param("monthKey") String monthKey);

    int insertBudget(@Param("budget") Budget budget,
                     @Param("userId") String userId,
                     @Param("monthKey") String monthKey);

    int updateBudget(@Param("budget") Budget budget);

    List<BudgetLine> listBudgetLines(@Param("budgetId") String budgetId);

    int insertBudgetLine(@Param("line") BudgetLine line, @Param("userId") String userId);

    int updateBudgetLine(@Param("line") BudgetLine line);

    int countBudgetLinesForUser(@Param("userId") String userId);

    List<Bill> listBills(@Param("userId") String userId);

    int insertBill(@Param("bill") Bill bill, @Param("userId") String userId);

    int updateBill(@Param("bill") Bill bill);

    int softDeleteBill(@Param("billId") String billId, @Param("userId") String userId);

    int countBillsForUser(@Param("userId") String userId);

    List<FinancialGoal> listGoals(@Param("userId") String userId);

    int insertGoal(@Param("goal") FinancialGoal goal, @Param("userId") String userId);

    int updateGoal(@Param("goal") FinancialGoal goal);

    int countGoalsForUser(@Param("userId") String userId);

    void upsertMetric(@Param("userId") String userId,
                      @Param("yearMonth") String yearMonth,
                      @Param("metricCode") String metricCode,
                      @Param("value") BigDecimal value);

    BigDecimal findMetric(@Param("userId") String userId,
                          @Param("yearMonth") String yearMonth,
                          @Param("metricCode") String metricCode);

    List<Map<String, Object>> listMetrics(@Param("userId") String userId,
                                          @Param("fromYm") String fromYm,
                                          @Param("toYm") String toYm);
}

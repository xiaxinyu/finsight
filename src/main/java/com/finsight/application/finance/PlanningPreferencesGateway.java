package com.finsight.application.finance;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.config.FinsightFeatureProperties;
import com.finsight.domain.model.Bill;
import com.finsight.domain.model.Budget;
import com.finsight.domain.model.BudgetLine;
import com.finsight.domain.model.FinancialGoal;
import com.finsight.domain.port.BillRepository;
import com.finsight.domain.port.BudgetRepository;
import com.finsight.domain.port.GoalRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Dual-run gateway: in-memory store (default) or DB persistence when {@code finsight.planning.persist=true}.
 */
@Component
public class PlanningPreferencesGateway {

    private static final Logger log = LoggerFactory.getLogger(PlanningPreferencesGateway.class);

    private final FinsightFeatureProperties features;
    private final AuthenticationFacade authenticationFacade;
    private final PlanningPreferencesStore memoryStore;
    private final BudgetRepository budgetRepository;
    private final BillRepository billRepository;
    private final GoalRepository goalRepository;

    public PlanningPreferencesGateway(FinsightFeatureProperties features,
                                      AuthenticationFacade authenticationFacade,
                                      PlanningPreferencesStore memoryStore,
                                      BudgetRepository budgetRepository,
                                      BillRepository billRepository,
                                      GoalRepository goalRepository) {
        this.features = features;
        this.authenticationFacade = authenticationFacade;
        this.memoryStore = memoryStore;
        this.budgetRepository = budgetRepository;
        this.billRepository = billRepository;
        this.goalRepository = goalRepository;
    }

    @PostConstruct
    public void bootstrapFromMemoryIfNeeded() {
        if (!features.getPlanning().isPersist()) {
            return;
        }
        String userId = userKey();
        if (budgetRepository.hasAnyForUser(userId)
                || billRepository.hasAnyForUser(userId)
                || goalRepository.hasAnyForUser(userId)) {
            return;
        }
        List<BudgetLine> memLines = memoryStore.budgetLinesForCurrentMonth();
        List<Bill> memBills = memoryStore.enabledBills();
        List<FinancialGoal> memGoals = memoryStore.goals();
        if (memLines.isEmpty() && memBills.isEmpty() && memGoals.isEmpty()) {
            return;
        }
        log.info("Importing in-memory planning data to DB for user={}", userId);
        Budget budget = currentMonthlyBudget();
        budgetRepository.saveBudget(budget, userId);
        for (BudgetLine line : memLines) {
            line.setBudgetId(budget.getId());
            budgetRepository.upsertLine(line, userId);
        }
        for (Bill bill : memBills) {
            billRepository.save(bill, userId);
        }
        for (FinancialGoal goal : memGoals) {
            goalRepository.save(goal, userId);
        }
    }

    public List<BudgetLine> budgetLinesForCurrentMonth() {
        if (!features.getPlanning().isPersist()) {
            return memoryStore.budgetLinesForCurrentMonth();
        }
        Budget budget = ensureDbBudget();
        return budgetRepository.linesForBudget(budget.getId());
    }

    public BudgetLine upsertBudgetLine(BudgetLine line) {
        if (!features.getPlanning().isPersist()) {
            return memoryStore.upsertBudgetLine(line);
        }
        Budget budget = ensureDbBudget();
        if (line.getBudgetId() == null || line.getBudgetId().isBlank()) {
            line.setBudgetId(budget.getId());
        }
        return budgetRepository.upsertLine(line, userKey());
    }

    public List<Bill> enabledBills() {
        if (!features.getPlanning().isPersist()) {
            return memoryStore.enabledBills();
        }
        return billRepository.listEnabled(userKey());
    }

    public Bill saveBill(Bill bill) {
        if (!features.getPlanning().isPersist()) {
            return memoryStore.saveBill(bill);
        }
        return billRepository.save(bill, userKey());
    }

    public void deleteBill(String billId) {
        if (billId == null || billId.isBlank()) {
            throw new IllegalArgumentException("Bill id is required");
        }
        if (!features.getPlanning().isPersist()) {
            memoryStore.deleteBill(billId);
            return;
        }
        billRepository.softDelete(billId, userKey());
    }

    public List<FinancialGoal> goals() {
        if (!features.getPlanning().isPersist()) {
            return memoryStore.goals();
        }
        return goalRepository.listActive(userKey());
    }

    public FinancialGoal saveGoal(FinancialGoal goal) {
        if (!features.getPlanning().isPersist()) {
            return memoryStore.saveGoal(goal);
        }
        return goalRepository.save(goal, userKey());
    }

    private Budget ensureDbBudget() {
        String userId = userKey();
        String monthKey = memoryStore.currentMonthKey();
        Budget existing = budgetRepository.findByUserAndMonth(userId, monthKey);
        if (existing != null) {
            return existing;
        }
        Budget budget = currentMonthlyBudget();
        return budgetRepository.saveBudget(budget, userId);
    }

    private Budget currentMonthlyBudget() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int year = cal.get(java.util.Calendar.YEAR);
        int month = cal.get(java.util.Calendar.MONTH) + 1;
        Budget b = new Budget();
        b.setId("monthly-" + year + "-" + month);
        b.setName(year + "-" + month + " Budget");
        b.setPeriodType("monthly");
        b.setYear(year);
        b.setMonth(month);
        b.setDeleted(0);
        return b;
    }

    private String userKey() {
        String user = authenticationFacade.getUserName();
        return user == null || user.isBlank() ? "_anonymous" : user;
    }
}

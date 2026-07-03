package com.finsight.application.finance;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.domain.model.Bill;
import com.finsight.domain.model.BudgetLine;
import com.finsight.domain.model.FinancialGoal;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory planning preferences (budget limits, bills, goals).
 * Not sourced from transaction imports; kept out of new DB tables per product direction.
 */
@Component
public class PlanningPreferencesStore {

    private final AuthenticationFacade authenticationFacade;
    private final Map<String, List<BudgetLine>> budgetLines = new ConcurrentHashMap<>();
    private final Map<String, List<Bill>> bills = new ConcurrentHashMap<>();
    private final Map<String, List<FinancialGoal>> goals = new ConcurrentHashMap<>();
    private final Map<String, int[]> incomePayDays = new ConcurrentHashMap<>();

    private static final int[] DEFAULT_INCOME_PAY_DAYS = {5, 20};

    public PlanningPreferencesStore(AuthenticationFacade authenticationFacade) {
        this.authenticationFacade = authenticationFacade;
    }

    public String currentMonthKey() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1);
    }

    public List<BudgetLine> budgetLinesForCurrentMonth() {
        return budgetLines.computeIfAbsent(userMonthKey(), k -> new ArrayList<>());
    }

    public BudgetLine upsertBudgetLine(BudgetLine line) {
        List<BudgetLine> lines = budgetLinesForCurrentMonth();
        if (line.getId() == null || line.getId().isBlank()) {
            line.setId(UUID.randomUUID().toString());
            lines.add(line);
            return line;
        }
        for (int i = 0; i < lines.size(); i++) {
            if (line.getId().equals(lines.get(i).getId())) {
                lines.set(i, line);
                return line;
            }
        }
        lines.add(line);
        return line;
    }

    public List<Bill> enabledBills() {
        return bills.computeIfAbsent(userKey(), k -> new ArrayList<>()).stream()
                .filter(b -> b.getDeleted() == null || b.getDeleted() == 0)
                .filter(b -> b.getEnabled() == null || b.getEnabled() == 1)
                .sorted((a, b2) -> {
                    int da = a.getDueDay() == null ? 0 : a.getDueDay();
                    int db = b2.getDueDay() == null ? 0 : b2.getDueDay();
                    return Integer.compare(da, db);
                })
                .toList();
    }

    public Bill saveBill(Bill bill) {
        List<Bill> list = bills.computeIfAbsent(userKey(), k -> new ArrayList<>());
        if (bill.getId() == null || bill.getId().isBlank()) {
            bill.setId(UUID.randomUUID().toString());
            if (bill.getEnabled() == null) {
                bill.setEnabled(1);
            }
            if (bill.getDeleted() == null) {
                bill.setDeleted(0);
            }
            list.add(bill);
            return bill;
        }
        for (int i = 0; i < list.size(); i++) {
            if (bill.getId().equals(list.get(i).getId())) {
                list.set(i, bill);
                return bill;
            }
        }
        list.add(bill);
        return bill;
    }

    public void deleteBill(String billId) {
        List<Bill> list = bills.computeIfAbsent(userKey(), k -> new ArrayList<>());
        for (int i = 0; i < list.size(); i++) {
            if (billId.equals(list.get(i).getId())) {
                Bill b = list.get(i);
                b.setDeleted(1);
                b.setEnabled(0);
                list.set(i, b);
                return;
            }
        }
    }

    public List<FinancialGoal> goals() {
        return goals.computeIfAbsent(userKey(), k -> new ArrayList<>()).stream()
                .filter(g -> g.getDeleted() == null || g.getDeleted() == 0)
                .toList();
    }

    public FinancialGoal saveGoal(FinancialGoal goal) {
        List<FinancialGoal> list = goals.computeIfAbsent(userKey(), k -> new ArrayList<>());
        if (goal.getId() == null || goal.getId().isBlank()) {
            goal.setId(UUID.randomUUID().toString());
            if (goal.getDeleted() == null) {
                goal.setDeleted(0);
            }
            list.add(goal);
            return goal;
        }
        for (int i = 0; i < list.size(); i++) {
            if (goal.getId().equals(list.get(i).getId())) {
                list.set(i, goal);
                return goal;
            }
        }
        list.add(goal);
        return goal;
    }

    public int[] getIncomePayDays() {
        return incomePayDays.getOrDefault(userKey(), DEFAULT_INCOME_PAY_DAYS).clone();
    }

    public java.util.List<Integer> setIncomePayDays(java.util.List<Integer> days) {
        if (days == null || days.isEmpty()) {
            throw new IllegalArgumentException("At least one pay day is required");
        }
        if (days.size() > 4) {
            throw new IllegalArgumentException("At most 4 pay days allowed");
        }
        int[] normalized = days.stream()
                .filter(d -> d != null)
                .mapToInt(Integer::intValue)
                .distinct()
                .sorted()
                .peek(d -> {
                    if (d < 1 || d > 28) {
                        throw new IllegalArgumentException("Pay day must be between 1 and 28");
                    }
                })
                .toArray();
        if (normalized.length == 0) {
            throw new IllegalArgumentException("At least one valid pay day is required");
        }
        incomePayDays.put(userKey(), normalized);
        return java.util.Arrays.stream(normalized).boxed().toList();
    }

    private String userKey() {
        String user = authenticationFacade.getUserName();
        return user == null || user.isBlank() ? "_anonymous" : user;
    }

    private String userMonthKey() {
        return userKey() + ":" + currentMonthKey();
    }
}

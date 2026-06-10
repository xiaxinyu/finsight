package com.finsight.application.transaction;

import com.finsight.domain.model.Transaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Mirrors SQL rowIncomeAmount / rowExpenseAmount semantics for report consistency checks.
 */
class TransactionReportAmountLogicTest {

    private static double rowIncomeAmount(Transaction t) {
        double income = t.getIncomeMoney() == null ? 0 : t.getIncomeMoney();
        double balance = t.getBalanceMoney() == null ? 0 : t.getBalanceMoney();
        if (income > 0) {
            return income;
        }
        if (balance < 0) {
            return Math.abs(balance);
        }
        return 0;
    }

    private static double rowExpenseAmount(Transaction t) {
        double balance = t.getBalanceMoney() == null ? 0 : t.getBalanceMoney();
        return balance > 0 ? balance : 0;
    }

    @Test
    void normalizedRow_splitsIncomeAndExpense() {
        Transaction income = new Transaction();
        income.setIncomeMoney(2000.0);
        income.setBalanceMoney(0.0);
        TransactionAmountNormalizer.normalize(income);
        assertEquals(2000.0, rowIncomeAmount(income));
        assertEquals(0.0, rowExpenseAmount(income));

        Transaction expense = new Transaction();
        expense.setBalanceMoney(88.0);
        TransactionAmountNormalizer.normalize(expense);
        assertEquals(0.0, rowIncomeAmount(expense));
        assertEquals(88.0, rowExpenseAmount(expense));
    }

    @Test
    void legacyNegativeBalance_countsAsIncomeOnly() {
        Transaction legacy = new Transaction();
        legacy.setBalanceMoney(-120.0);
        TransactionAmountNormalizer.normalize(legacy);
        assertEquals(120.0, rowIncomeAmount(legacy));
        assertEquals(0.0, rowExpenseAmount(legacy));
    }
}

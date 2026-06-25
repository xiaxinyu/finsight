package com.finsight.application.transaction;

import com.finsight.domain.model.Transaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionAmountNormalizerTest {

    @Test
    void normalizesSignedExpenseToPositiveBalance() {
        Transaction t = new Transaction();
        t.setBalanceMoney(-120.5);
        TransactionAmountNormalizer.normalize(t);
        assertEquals(120.5, t.getIncomeMoney());
        assertEquals(0.0, t.getBalanceMoney());
    }

    @Test
    void normalizesIncomeRowWithBothFieldsSet() {
        Transaction t = new Transaction();
        t.setIncomeMoney(500.0);
        t.setBalanceMoney(500.0);
        TransactionAmountNormalizer.normalize(t);
        assertEquals(500.0, t.getIncomeMoney());
        assertEquals(0.0, t.getBalanceMoney());
    }

    @Test
    void keepsCanonicalExpenseRow() {
        Transaction t = new Transaction();
        t.setBalanceMoney(88.0);
        TransactionAmountNormalizer.normalize(t);
        assertEquals(88.0, t.getBalanceMoney());
        assertEquals(0.0, t.getIncomeMoney());
    }

    @Test
    void applyTxnKind_expenseToIncome_preservesMagnitude() {
        Transaction t = new Transaction();
        t.setBalanceMoney(1500.0);
        t.setTxnKind("expense");
        TransactionAmountNormalizer.applyTxnKind(t, "income");
        assertEquals(1500.0, t.getIncomeMoney());
        assertEquals(0.0, t.getBalanceMoney());
        assertEquals("income", t.getTxnKind());
    }

    @Test
    void applyTxnKind_incomeToExpense_preservesMagnitude() {
        Transaction t = new Transaction();
        t.setIncomeMoney(1500.0);
        t.setTxnKind("income");
        TransactionAmountNormalizer.applyTxnKind(t, "expense");
        assertEquals(1500.0, t.getBalanceMoney());
        assertEquals(0.0, t.getIncomeMoney());
        assertEquals("expense", t.getTxnKind());
    }

    @Test
    void applyTxnKind_doesNotSumBothColumns() {
        Transaction t = new Transaction();
        t.setIncomeMoney(1500.0);
        t.setBalanceMoney(500.0);
        TransactionAmountNormalizer.applyTxnKind(t, "expense");
        assertEquals(1500.0, t.getBalanceMoney());
        assertEquals(0.0, t.getIncomeMoney());
    }
}

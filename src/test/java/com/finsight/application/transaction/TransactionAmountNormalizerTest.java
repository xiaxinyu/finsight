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
}

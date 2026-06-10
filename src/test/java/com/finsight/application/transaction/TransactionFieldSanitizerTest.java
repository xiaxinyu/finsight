package com.finsight.application.transaction;

import com.finsight.domain.model.Transaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TransactionFieldSanitizerTest {

    @Test
    void truncatesOpponentFieldsToColumnLimits() {
        Transaction t = new Transaction();
        t.setOpponentName("n".repeat(200));
        t.setOpponentAccount("a".repeat(80));

        TransactionFieldSanitizer.sanitize(t);

        assertEquals(128, t.getOpponentName().length());
        assertEquals(64, t.getOpponentAccount().length());
    }

    @Test
    void leavesShortValuesUnchanged() {
        Transaction t = new Transaction();
        t.setOpponentName("财付通");
        t.setOpponentAccount("6222021234567890");

        TransactionFieldSanitizer.sanitize(t);

        assertEquals("财付通", t.getOpponentName());
        assertEquals("6222021234567890", t.getOpponentAccount());
    }

    @Test
    void handlesNullFields() {
        Transaction t = new Transaction();
        TransactionFieldSanitizer.sanitize(t);
        assertNull(t.getOpponentName());
        assertNull(t.getOpponentAccount());
    }
}

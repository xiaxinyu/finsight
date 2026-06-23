package com.finsight.application.transaction;

import com.finsight.domain.model.Transaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionCategoryFieldSyncTest {

    @Test
    void applyCategoryFieldsSyncsLegacyAndCanonicalColumns() {
        Transaction t = new Transaction();

        TransactionCategoryFieldSync.applyCategoryFields(t, "DAILY-01", "Daily food");

        assertEquals("DAILY-01", t.getConsumeCode());
        assertEquals("DAILY-01", t.getCategoryCode());
        assertEquals("DAILY-01", t.getConsumeID());
        assertEquals("DAILY-01", t.getCategoryId());
        assertEquals("Daily food", t.getConsumeName());
        assertEquals("Daily food", t.getCategoryName());
    }

    @Test
    void resolveCanonicalCodeReturnsTrimmedCode() {
        Transaction t = new Transaction();
        t.setConsumeCode("  DAILY-01  ");

        assertEquals("DAILY-01", TransactionCategoryFieldSync.resolveCanonicalCode(t));
    }

    @Test
    void clearCategoryFieldsClearsAllColumns() {
        Transaction t = new Transaction();
        TransactionCategoryFieldSync.applyCategoryFields(t, "DAILY-01", "Daily food");

        TransactionCategoryFieldSync.clearCategoryFields(t);

        assertNull(t.getConsumeCode());
        assertNull(t.getCategoryCode());
        assertNull(t.getConsumeID());
        assertNull(t.getCategoryId());
        assertNull(t.getConsumeName());
        assertNull(t.getCategoryName());
    }

    @Test
    void hasCategoryFieldPatchDetectsExplicitFieldUpdates() {
        Transaction t = new Transaction();
        assertFalse(t.hasCategoryFieldPatch());

        t.setConsumeCode("DAILY-01");
        assertTrue(t.hasCategoryFieldPatch());
    }
}

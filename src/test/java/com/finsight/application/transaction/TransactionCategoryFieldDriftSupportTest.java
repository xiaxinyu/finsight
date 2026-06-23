package com.finsight.application.transaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionCategoryFieldDriftSupportTest {

    @Test
    void detectsNameDrift() {
        assertTrue(TransactionCategoryFieldDriftSupport.isDrift(
                "DAILY-01", "DAILY-01", "Old name", "DAILY-01", "Old name", "DAILY-01",
                "DAILY-01", "Daily food", "cat-id"));
    }

    @Test
    void detectsLegacyIdDrift() {
        assertTrue(TransactionCategoryFieldDriftSupport.isDrift(
                "DAILY-01", "legacy-id", "Daily food", "DAILY-01", "Daily food", "legacy-id",
                "DAILY-01", "Daily food", "cat-id"));
    }

    @Test
    void alignedRowIsNotDrift() {
        assertFalse(TransactionCategoryFieldDriftSupport.isDrift(
                "DAILY-01", "DAILY-01", "Daily food", "DAILY-01", "Daily food", "DAILY-01",
                "DAILY-01", "Daily food", "cat-id"));
    }

    @Test
    void allowsCategoryIdMatchingCatalogId() {
        assertFalse(TransactionCategoryFieldDriftSupport.isDrift(
                "DAILY-01", "DAILY-01", "Daily food", "DAILY-01", "Daily food", "cat-id",
                "DAILY-01", "Daily food", "cat-id"));
    }
}

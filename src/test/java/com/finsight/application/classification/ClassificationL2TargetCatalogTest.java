package com.finsight.application.classification;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationL2TargetCatalogTest {

    @Test
    void catalogCodesAreUnique() {
        L2CategorySeedPlanner.validateCatalog();
        long distinct = java.util.Arrays.stream(ClassificationL2TargetCatalog.values())
                .map(ClassificationL2TargetCatalog::code)
                .distinct()
                .count();
        assertEquals(ClassificationL2TargetCatalog.values().length, distinct);
    }

    @Test
    void everyParentIsKnownL1() {
        for (ClassificationL2TargetCatalog target : ClassificationL2TargetCatalog.values()) {
            assertTrue(ClassificationL1Codes.isKnownL1(target.parentL1Code()),
                    () -> "Unknown parent for " + target.code());
        }
    }

    @Test
    void insertableBatchExcludesCatalogOnlyEntries() {
        Set<String> catalogOnly = java.util.Arrays.stream(ClassificationL2TargetCatalog.values())
                .filter(c -> !c.insertWhenMissing())
                .map(ClassificationL2TargetCatalog::code)
                .collect(Collectors.toSet());
        assertTrue(catalogOnly.contains("OTHER-01"));
        assertTrue(catalogOnly.contains("TRAVEL-01"));
        assertFalse(ClassificationL2TargetCatalog.insertableBatch().stream()
                .anyMatch(c -> catalogOnly.contains(c.code())));
    }

    @Test
    void nonExpenseCategoriesHaveReportRole() {
        assertEquals("refund", ClassificationL2TargetCatalog.REIM_CORP.reportRole());
        assertEquals("transfer", ClassificationL2TargetCatalog.ASSET_INTERNAL.reportRole());
        assertEquals("liability", ClassificationL2TargetCatalog.DEBT_CC_REPAY.reportRole());
        assertEquals("investment", ClassificationL2TargetCatalog.INV_STOCK_BUY.reportRole());
    }
}

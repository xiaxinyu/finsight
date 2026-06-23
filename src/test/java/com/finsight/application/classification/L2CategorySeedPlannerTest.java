package com.finsight.application.classification;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class L2CategorySeedPlannerTest {

    @Test
    void buildInsertPlanSkipsExistingAndCatalogOnly() {
        Set<String> existing = Set.of("DAILY-02", "INVEST-01", "OTHER-01");
        var plan = L2CategorySeedPlanner.buildInsertPlan(existing);

        assertTrue(plan.stream().anyMatch(i -> i.code().equals("DAILY-02")
                && i.action() == L2CategorySeedPlanner.Action.SKIP_EXISTS));
        assertTrue(plan.stream().anyMatch(i -> i.code().equals("INVEST-01")
                && i.action() == L2CategorySeedPlanner.Action.SKIP_CATALOG_ONLY));
        assertTrue(plan.stream().anyMatch(i -> i.code().equals("DAILY-03")
                && i.action() == L2CategorySeedPlanner.Action.INSERT));
    }

    @Test
    void countInsertsMatchesEmptyDatabase() {
        long inserts = L2CategorySeedPlanner.countInserts(Set.of());
        assertEquals(ClassificationL2TargetCatalog.insertableBatch().size(), inserts);
    }

    @Test
    void nameUpdatesNeverChangeCode() {
        assertTrue(L2CategorySeedPlanner.buildNameUpdates().stream()
                .allMatch(u -> u.code() != null && !u.code().isBlank()));
    }

    @Test
    void insertPlanUsesCanonicalTransportParentOnFreshDb() {
        var trans = L2CategorySeedPlanner.buildInsertPlan(Set.of()).stream()
                .filter(i -> "TRANS-02".equals(i.code()))
                .findFirst()
                .orElseThrow();
        assertEquals("TRANSPORT", trans.parentL1Code());
    }

    @Test
    void insertPlanUsesTravelWhenOnlyLegacyTransportL1Exists() {
        var trans = L2CategorySeedPlanner.buildInsertPlan(Set.of("TRAVEL")).stream()
                .filter(i -> "TRANS-02".equals(i.code()))
                .findFirst()
                .orElseThrow();
        assertEquals("TRAVEL", trans.parentL1Code());
    }

    @Test
    void insertPlanUsesIncParentWhenOnlyLegacyIncomeL1Exists() {
        var side = L2CategorySeedPlanner.buildInsertPlan(Set.of("INCOME")).stream()
                .filter(i -> "INCOME-02".equals(i.code()))
                .findFirst()
                .orElseThrow();
        assertEquals(L2CategorySeedPlanner.Action.SKIP_CATALOG_ONLY, side.action());
        assertEquals("INCOME", side.parentL1Code());
    }
}

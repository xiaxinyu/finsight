package com.finsight.application.classification;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationL1CodesTest {

    @Test
    void resolveIncomeL1PrefersInc() {
        assertEquals("INC", ClassificationL1Codes.resolveIncomeL1(Set.of("INC", "INCOME")));
        assertEquals("INCOME", ClassificationL1Codes.resolveIncomeL1(Set.of("INCOME")));
        assertEquals("INC", ClassificationL1Codes.resolveIncomeL1(Set.of()));
    }

    @Test
    void resolveTransportL1PrefersTransport() {
        assertEquals("TRANSPORT", ClassificationL1Codes.resolveTransportL1(Set.of("TRAVEL", "TRANSPORT")));
        assertEquals("TRAVEL", ClassificationL1Codes.resolveTransportL1(Set.of("TRAVEL")));
        assertEquals("TRANSPORT", ClassificationL1Codes.resolveTransportL1(Set.of()));
    }

    @Test
    void resolveParentL1ForCatalogParents() {
        assertEquals("INC", ClassificationL1Codes.resolveParentL1("INC", Set.of("INC")));
        assertEquals("TRANSPORT", ClassificationL1Codes.resolveParentL1("TRANSPORT", Set.of("TRANSPORT")));
        assertTrue(ClassificationL1Codes.isKnownL1("TRANSPORT"));
    }
}

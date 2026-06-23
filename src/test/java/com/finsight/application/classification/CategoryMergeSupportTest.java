package com.finsight.application.classification;

import com.finsight.domain.model.ConsumeCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryMergeSupportTest {

    @Test
    void resolvesL1IntoL1() {
        assertEquals(CategoryMergeSupport.MergeMode.L1_INTO_L1,
                CategoryMergeSupport.resolveMode(l1("INCOME"), l1("INC")));
    }

    @Test
    void resolvesL2IntoL2() {
        assertEquals(CategoryMergeSupport.MergeMode.L2_INTO_L2,
                CategoryMergeSupport.resolveMode(l2("INCOME-01", "INCOME"), l2("INC-01", "INC")));
    }

    @Test
    void resolvesL2ReparentToL1() {
        assertEquals(CategoryMergeSupport.MergeMode.L2_REPARENT_TO_L1,
                CategoryMergeSupport.resolveMode(l2("INCOME-02", "INCOME"), l1("INC")));
    }

    @Test
    void rejectsL1IntoL2() {
        assertThrows(IllegalArgumentException.class,
                () -> CategoryMergeSupport.resolveMode(l1("INCOME"), l2("INC-01", "INC")));
    }

    @Test
    void detectsKnownDuplicateL1Pair() {
        assertTrue(CategoryMergeSupport.isKnownDuplicateL1Pair("INCOME", "INC"));
        assertTrue(CategoryMergeSupport.isKnownDuplicateL1Pair("TRAVEL", "TRANSPORT"));
    }

    private static ConsumeCategory l1(String code) {
        ConsumeCategory c = new ConsumeCategory();
        c.setCode(code);
        c.setId(code);
        c.setLevel(1);
        c.setParentId(null);
        return c;
    }

    private static ConsumeCategory l2(String code, String parent) {
        ConsumeCategory c = new ConsumeCategory();
        c.setCode(code);
        c.setId(code);
        c.setLevel(2);
        c.setParentId(parent);
        return c;
    }
}

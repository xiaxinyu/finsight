package com.finsight.application.classification;

import com.finsight.domain.model.ConsumeCategory;
import org.apache.commons.lang3.StringUtils;

/**
 * Classifies category merge/migrate scenarios for {@code ConsumeCategoryAdminFacade}.
 */
public final class CategoryMergeSupport {

    public enum MergeMode {
        /** Duplicate L1 roots: reparent children, soft-delete source L1. */
        L1_INTO_L1,
        /** Move an L2 under a different L1 parent. */
        L2_REPARENT_TO_L1,
        /** Merge L2 into L2: remap transactions and rules. */
        L2_INTO_L2
    }

    private CategoryMergeSupport() {
    }

    public static MergeMode resolveMode(ConsumeCategory source, ConsumeCategory target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Source and target categories are required");
        }
        boolean srcL1 = isLevelOne(source);
        boolean tgtL1 = isLevelOne(target);
        if (srcL1 && tgtL1) {
            return MergeMode.L1_INTO_L1;
        }
        if (srcL1 && !tgtL1) {
            throw new IllegalArgumentException("Cannot merge an L1 category into an L2 category");
        }
        if (!srcL1 && tgtL1) {
            return MergeMode.L2_REPARENT_TO_L1;
        }
        return MergeMode.L2_INTO_L2;
    }

    public static boolean isLevelOne(ConsumeCategory cat) {
        if (cat == null) {
            return false;
        }
        Integer level = cat.getLevel();
        if (level != null && level == 1) {
            return true;
        }
        return StringUtils.isBlank(cat.getParentId());
    }

    /** Pairs of duplicate L1 names from v1.8 Sprint2 seed vs legacy installs. */
    public static boolean isKnownDuplicateL1Pair(String sourceCode, String targetCode) {
        if (sourceCode == null || targetCode == null) {
            return false;
        }
        String src = sourceCode.trim();
        String tgt = targetCode.trim();
        return ("INCOME".equalsIgnoreCase(src) && "INC".equalsIgnoreCase(tgt))
                || ("INC".equalsIgnoreCase(src) && "INCOME".equalsIgnoreCase(tgt))
                || ("TRAVEL".equalsIgnoreCase(src) && "TRANSPORT".equalsIgnoreCase(tgt))
                || ("TRANSPORT".equalsIgnoreCase(src) && "TRAVEL".equalsIgnoreCase(tgt));
    }
}

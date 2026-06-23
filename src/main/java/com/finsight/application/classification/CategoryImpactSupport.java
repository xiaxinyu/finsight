package com.finsight.application.classification;

import com.finsight.domain.model.ConsumeCategory;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared helpers for category impact preview (transaction match keys, report labels).
 */
public final class CategoryImpactSupport {

    public static final List<String> REPORT_SURFACES = List.of(
            "Cashflow & Budget vs Actual",
            "Spending Drift",
            "Trend Changes",
            "Annual Outlook",
            "Merchant reports",
            "Financial Profile",
            "Home dashboard buckets"
    );

    private CategoryImpactSupport() {
    }

    public static List<String> categoryRefs(ConsumeCategory cat) {
        Set<String> refs = new LinkedHashSet<>();
        if (cat == null) {
            return List.of();
        }
        addRef(refs, cat.getCode());
        addRef(refs, cat.getId());
        return List.copyOf(refs);
    }

    public static String transactionMatchSql(List<String> refs) {
        if (refs == null || refs.isEmpty()) {
            return "1=0";
        }
        List<String> clauses = new ArrayList<>();
        for (int i = 0; i < refs.size(); i++) {
            clauses.add("consume_code = ?");
            clauses.add("consume_id = ?");
            clauses.add("category_code = ?");
            clauses.add("category_id = ?");
        }
        return "(" + String.join(" OR ", clauses) + ")";
    }

    public static Object[] transactionMatchParams(List<String> refs) {
        if (refs == null || refs.isEmpty()) {
            return new Object[0];
        }
        List<Object> params = new ArrayList<>();
        for (String ref : refs) {
            for (int i = 0; i < 4; i++) {
                params.add(ref);
            }
        }
        return params.toArray();
    }

    public static List<String> warningsFor(CategoryImpactAction action,
                                           long childCount,
                                           long transactionCount,
                                           long activeRuleCount,
                                           String targetCode) {
        List<String> warnings = new ArrayList<>();
        if (childCount > 0) {
            warnings.add("Category has " + childCount + " child categories — resolve or reparent them first.");
        }
        if (action == CategoryImpactAction.DELETE) {
            if (transactionCount > 0) {
                warnings.add(transactionCount + " transactions will keep this code but the category becomes inactive; reports may show stale labels until remapped.");
            }
            if (activeRuleCount > 0) {
                warnings.add(activeRuleCount + " active rules will be turned off.");
            }
        }
        if (action == CategoryImpactAction.RENAME && transactionCount > 0) {
            warnings.add("Stored transaction consume_name values are not auto-updated — run category field sync or edit if labels must match immediately.");
        }
        if (action == CategoryImpactAction.MERGE) {
            if (StringUtils.isBlank(targetCode)) {
                warnings.add("Merge requires a target category code.");
            } else if (transactionCount > 0) {
                warnings.add("Merge will update " + transactionCount
                        + " transactions and remap active rules to the target code.");
            }
            if (activeRuleCount > 0) {
                warnings.add(activeRuleCount + " active rules on the source will point to the target after merge.");
            }
        }
        return warnings;
    }

    private static void addRef(Set<String> refs, String value) {
        String trimmed = StringUtils.trimToNull(value);
        if (trimmed != null) {
            refs.add(trimmed);
        }
    }
}

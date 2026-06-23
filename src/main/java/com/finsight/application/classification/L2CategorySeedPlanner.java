package com.finsight.application.classification;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds L2 category insert/name-update plans from {@link ClassificationL2TargetCatalog}.
 */
public final class L2CategorySeedPlanner {

    public enum Action {
        INSERT, SKIP_EXISTS, SKIP_CATALOG_ONLY, UPDATE_NAME
    }

    public record SeedItem(
            Action action,
            String code,
            String name,
            String parentL1Code,
            int sortNo,
            String txnTypes,
            String reportRole,
            String reason) {
    }

    public record NameUpdate(String code, String newName, String previousNameHint, String reason, Integer level) {
        public NameUpdate(String code, String newName, String previousNameHint, String reason) {
            this(code, newName, previousNameHint, reason, null);
        }
    }

    private L2CategorySeedPlanner() {
    }

    public static List<SeedItem> buildInsertPlan(Set<String> existingCodes) {
        Set<String> existing = existingCodes == null ? Set.of() : existingCodes;
        List<SeedItem> items = new ArrayList<>();
        for (ClassificationL2TargetCatalog target : ClassificationL2TargetCatalog.values()) {
            if (!target.insertWhenMissing()) {
                items.add(new SeedItem(
                        Action.SKIP_CATALOG_ONLY,
                        target.code(),
                        target.displayName(),
                        target.parentL1Code(),
                        target.sortNo(),
                        target.txnTypes(),
                        target.reportRole(),
                        "Documents existing code — not inserted by seed script"));
                continue;
            }
            if (existing.contains(target.code())) {
                items.add(new SeedItem(
                        Action.SKIP_EXISTS,
                        target.code(),
                        target.displayName(),
                        target.parentL1Code(),
                        target.sortNo(),
                        target.txnTypes(),
                        target.reportRole(),
                        "Code already in database"));
            } else {
                items.add(new SeedItem(
                        Action.INSERT,
                        target.code(),
                        target.displayName(),
                        target.parentL1Code(),
                        target.sortNo(),
                        target.txnTypes(),
                        target.reportRole(),
                        "New L2 from v1.8 §1.3 target catalog"));
            }
        }
        items.sort(Comparator.comparing(SeedItem::action).thenComparing(SeedItem::parentL1Code)
                .thenComparing(SeedItem::sortNo));
        return items;
    }

    public static List<NameUpdate> buildNameUpdates() {
        return List.of(
                new NameUpdate("OTHER-01", "临时无法归类", "无法归类的支出", "Reduce ambiguity vs OTHER L1 catch-all"),
                new NameUpdate("INVEST-OTHER", "投资未分类", "其它消费", "Distinguish from expense OTHER category"),
                new NameUpdate("OTHER", "其它消费", "其他类别", "Align L1 name with v1.8 §1.3", 1));
    }

    public static void validateCatalog() {
        Set<String> codes = ClassificationL2TargetCatalog.values().length == 0
                ? Set.of()
                : java.util.Arrays.stream(ClassificationL2TargetCatalog.values())
                        .map(ClassificationL2TargetCatalog::code)
                        .collect(Collectors.toSet());
        if (codes.size() != ClassificationL2TargetCatalog.values().length) {
            throw new IllegalStateException("Duplicate L2 category codes in catalog");
        }
        for (ClassificationL2TargetCatalog target : ClassificationL2TargetCatalog.values()) {
            if (!ClassificationL1Codes.isKnownL1(target.parentL1Code())) {
                throw new IllegalStateException("Unknown parent L1: " + target.parentL1Code()
                        + " for " + target.code());
            }
        }
    }

    public static long countInserts(Set<String> existingCodes) {
        return buildInsertPlan(existingCodes).stream()
                .filter(i -> i.action() == Action.INSERT)
                .count();
    }
}

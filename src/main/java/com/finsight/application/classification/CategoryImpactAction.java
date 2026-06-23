package com.finsight.application.classification;

/**
 * Category destructive action for impact preview.
 */
public enum CategoryImpactAction {
    DELETE,
    RENAME,
    MERGE;

    public static CategoryImpactAction parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return DELETE;
        }
        return switch (raw.trim().toLowerCase()) {
            case "rename" -> RENAME;
            case "merge" -> MERGE;
            default -> DELETE;
        };
    }
}

package com.finsight.application.classification;

/**
 * Rule quality risks aligned with classification-data-audit.sql §6–§7.
 */
public enum RuleRiskKind {
    DUPLICATE_PATTERN,
    CROSS_CATEGORY_CONFLICT,
    BROAD_KEYWORD,
    DIRECTION_MISMATCH,
    ORPHAN_CATEGORY,
    INVALID_PATTERN,
    NO_CATEGORY
}

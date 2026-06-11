package com.finsight.application.statement;

import java.util.List;

/**
 * Import line accounting: lines = linked + skipped + ignored.
 * {@code transactions} is parsed txn count (may be less than {@code linked} when rows merge).
 */
public record ImportLineStats(
        int lines,
        int transactions,
        int linked,
        int skipped,
        int ignored,
        List<SkippedImportRow> skippedRows) {
}

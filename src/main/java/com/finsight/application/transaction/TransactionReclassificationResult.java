package com.finsight.application.transaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TransactionReclassificationResult {

    private int requested;
    private int classified;
    private int skipped;
    private int noMatch;
    private boolean dryRun;
    private final List<Map<String, Object>> preview = new ArrayList<>();

    public int getRequested() {
        return requested;
    }

    public void setRequested(int requested) {
        this.requested = requested;
    }

    public int getClassified() {
        return classified;
    }

    public void setClassified(int classified) {
        this.classified = classified;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public int getNoMatch() {
        return noMatch;
    }

    public void setNoMatch(int noMatch) {
        this.noMatch = noMatch;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public List<Map<String, Object>> getPreview() {
        return preview;
    }

    public void addPreview(String id, String categoryCode, String categoryName, String action) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("categoryCode", categoryCode);
        row.put("categoryName", categoryName);
        row.put("action", action);
        preview.add(row);
    }
}

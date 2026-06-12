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
    private int suggested;
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

    public int getSuggested() {
        return suggested;
    }

    public void setSuggested(int suggested) {
        this.suggested = suggested;
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
        addPreview(id, categoryCode, categoryName, action, null, null);
    }

    public void addPreview(String id, String categoryCode, String categoryName, String action,
                           String transactionDesc, java.util.Date transactionDate) {
        addPreview(id, categoryCode, categoryName, action, transactionDesc, transactionDate, null, null, null, null);
    }

    public void addPreview(String id, String categoryCode, String categoryName, String action,
                           String transactionDesc, java.util.Date transactionDate,
                           String source, Integer confidence, String reason, java.util.List<String> suggestedKeywords) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("categoryCode", categoryCode);
        row.put("categoryName", categoryName);
        row.put("action", action);
        if (transactionDesc != null) {
            row.put("transactionDesc", transactionDesc);
        }
        if (transactionDate != null) {
            row.put("transactionDate", transactionDate);
        }
        if (source != null) {
            row.put("source", source);
        }
        if (confidence != null) {
            row.put("confidence", confidence);
        }
        if (reason != null) {
            row.put("reason", reason);
        }
        if (suggestedKeywords != null && !suggestedKeywords.isEmpty()) {
            row.put("suggestedKeywords", suggestedKeywords);
        }
        preview.add(row);
    }
}

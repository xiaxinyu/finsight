package com.finsight.application.transaction;

import com.finsight.domain.model.DrillBreakdownItem;
import com.finsight.domain.model.Transaction;

import java.util.ArrayList;
import java.util.List;

/** Drill-down payload: accurate aggregates plus a bounded transaction sample. */
public class DrillBreakdownResult {

    public static final int DEFAULT_SAMPLE_LIMIT = 200;

    private int total;
    private int sampleSize;
    private boolean truncated;
    private double aggregateTotal;
    private List<DrillBreakdownItem> categories = new ArrayList<>();
    private List<DrillBreakdownItem> merchants = new ArrayList<>();
    private List<Transaction> transactions = new ArrayList<>();

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public double getAggregateTotal() {
        return aggregateTotal;
    }

    public void setAggregateTotal(double aggregateTotal) {
        this.aggregateTotal = aggregateTotal;
    }

    public List<DrillBreakdownItem> getCategories() {
        return categories;
    }

    public void setCategories(List<DrillBreakdownItem> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
    }

    public List<DrillBreakdownItem> getMerchants() {
        return merchants;
    }

    public void setMerchants(List<DrillBreakdownItem> merchants) {
        this.merchants = merchants != null ? merchants : new ArrayList<>();
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
    }
}

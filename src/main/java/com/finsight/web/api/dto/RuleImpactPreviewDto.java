package com.finsight.web.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class RuleImpactPreviewDto {
    private String scope;
    private String draftCategoryId;
    private String draftCategoryCode;
    private String draftCategoryName;
    private long matchedCount;
    private double matchedAmount;
    private long unclassifiedMatchCount;
    private long wouldOverrideCount;
    private List<CategoryImpactRow> beforeByCategory = new ArrayList<>();
    private List<CategoryImpactRow> afterByCategory = new ArrayList<>();
    private List<SampleRow> samples = new ArrayList<>();

    @Getter
    @Setter
    public static class CategoryImpactRow {
        private String categoryCode;
        private String categoryName;
        private long txnCount;
        private double amount;
    }

    @Getter
    @Setter
    public static class SampleRow {
        private String transactionId;
        private Date transactionDate;
        private String description;
        private double amount;
        private String beforeCategoryCode;
        private String beforeCategoryName;
        private String afterCategoryCode;
        private String afterCategoryName;
        private boolean unclassified;
        private boolean wouldOverride;
        private String priorityExplanation;
        private List<CandidateHit> candidates = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class CandidateHit {
        private String categoryCode;
        private String categoryName;
        private Integer priority;
        private boolean winner;
    }
}

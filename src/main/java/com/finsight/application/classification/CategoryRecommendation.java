package com.finsight.application.classification;

import com.finsight.application.consume.ClassificationService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Suggested category when strict rule matching fails (similar transactions, weak rules, heuristics).
 */
public class CategoryRecommendation {

    public enum Source {
        WEAK_RULE,
        SIMILAR,
        HEURISTIC,
        KEYWORDS
    }

    private String categoryCode;
    private String categoryName;
    private Source source;
    private int confidence;
    private String reason;
    private final List<String> suggestedKeywords = new ArrayList<>();

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<String> getSuggestedKeywords() {
        return suggestedKeywords;
    }

    public boolean hasCategory() {
        return categoryCode != null && !categoryCode.isBlank();
    }

    public ClassificationService.Result toMatchResult() {
        if (!hasCategory()) {
            return null;
        }
        ClassificationService.Result r = new ClassificationService.Result();
        r.id = categoryCode;
        r.name = categoryName;
        return r;
    }

    public static CategoryRecommendation keywordsOnly(List<String> keywords, String reason) {
        CategoryRecommendation r = new CategoryRecommendation();
        r.setSource(Source.KEYWORDS);
        r.setConfidence(0);
        r.setReason(reason);
        if (keywords != null) {
            r.getSuggestedKeywords().addAll(keywords);
        }
        return r;
    }

    public List<String> keywordSnapshot() {
        return Collections.unmodifiableList(suggestedKeywords);
    }
}

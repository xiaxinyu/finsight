package com.finsight.web.api.dto;

import java.util.ArrayList;
import java.util.List;

public class RuleRiskEntryDto {

    private String ruleId;
    private String pattern;
    private String categoryId;
    private Integer priority;
    private Integer active;
    private List<String> risks = new ArrayList<>();
    private String suggestion;
    private boolean highRisk;
    private String duplicateGroupKey;
    private List<String> duplicatePeerRuleIds = new ArrayList<>();
    private List<String> duplicatePeerCategoryIds = new ArrayList<>();

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getActive() {
        return active;
    }

    public void setActive(Integer active) {
        this.active = active;
    }

    public List<String> getRisks() {
        return risks;
    }

    public void setRisks(List<String> risks) {
        this.risks = risks == null ? new ArrayList<>() : risks;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public boolean isHighRisk() {
        return highRisk;
    }

    public void setHighRisk(boolean highRisk) {
        this.highRisk = highRisk;
    }

    public String getDuplicateGroupKey() {
        return duplicateGroupKey;
    }

    public void setDuplicateGroupKey(String duplicateGroupKey) {
        this.duplicateGroupKey = duplicateGroupKey;
    }

    public List<String> getDuplicatePeerRuleIds() {
        return duplicatePeerRuleIds;
    }

    public void setDuplicatePeerRuleIds(List<String> duplicatePeerRuleIds) {
        this.duplicatePeerRuleIds = duplicatePeerRuleIds == null ? new ArrayList<>() : duplicatePeerRuleIds;
    }

    public List<String> getDuplicatePeerCategoryIds() {
        return duplicatePeerCategoryIds;
    }

    public void setDuplicatePeerCategoryIds(List<String> duplicatePeerCategoryIds) {
        this.duplicatePeerCategoryIds = duplicatePeerCategoryIds == null ? new ArrayList<>() : duplicatePeerCategoryIds;
    }
}

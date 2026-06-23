package com.finsight.web.api.dto;

import java.util.ArrayList;
import java.util.List;

public class RuleRiskRemediationItemDto {

    private String ruleId;
    private String pattern;
    private String categoryId;
    private Integer priority;
    private List<String> risks = new ArrayList<>();
    private String suggestedAction;

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

    public List<String> getRisks() {
        return risks;
    }

    public void setRisks(List<String> risks) {
        this.risks = risks == null ? new ArrayList<>() : risks;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
    }
}

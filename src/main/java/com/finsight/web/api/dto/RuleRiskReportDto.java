package com.finsight.web.api.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RuleRiskReportDto {

    private long activeRuleCount;
    private long highRiskRuleCount;
    private long duplicatePatternGroupCount;
    private long crossCategoryConflictRuleCount;
    private long broadKeywordRuleCount;
    private long directionMismatchRuleCount;
    private long orphanRuleCount;
    private long invalidPatternRuleCount;
    private List<RuleRiskEntryDto> entries = new ArrayList<>();
    private List<Map<String, Object>> duplicateGroups = new ArrayList<>();
    private List<RuleRiskRemediationItemDto> remediation = new ArrayList<>();

    public long getActiveRuleCount() {
        return activeRuleCount;
    }

    public void setActiveRuleCount(long activeRuleCount) {
        this.activeRuleCount = activeRuleCount;
    }

    public long getHighRiskRuleCount() {
        return highRiskRuleCount;
    }

    public void setHighRiskRuleCount(long highRiskRuleCount) {
        this.highRiskRuleCount = highRiskRuleCount;
    }

    public long getDuplicatePatternGroupCount() {
        return duplicatePatternGroupCount;
    }

    public void setDuplicatePatternGroupCount(long duplicatePatternGroupCount) {
        this.duplicatePatternGroupCount = duplicatePatternGroupCount;
    }

    public long getCrossCategoryConflictRuleCount() {
        return crossCategoryConflictRuleCount;
    }

    public void setCrossCategoryConflictRuleCount(long crossCategoryConflictRuleCount) {
        this.crossCategoryConflictRuleCount = crossCategoryConflictRuleCount;
    }

    public long getBroadKeywordRuleCount() {
        return broadKeywordRuleCount;
    }

    public void setBroadKeywordRuleCount(long broadKeywordRuleCount) {
        this.broadKeywordRuleCount = broadKeywordRuleCount;
    }

    public long getDirectionMismatchRuleCount() {
        return directionMismatchRuleCount;
    }

    public void setDirectionMismatchRuleCount(long directionMismatchRuleCount) {
        this.directionMismatchRuleCount = directionMismatchRuleCount;
    }

    public long getOrphanRuleCount() {
        return orphanRuleCount;
    }

    public void setOrphanRuleCount(long orphanRuleCount) {
        this.orphanRuleCount = orphanRuleCount;
    }

    public long getInvalidPatternRuleCount() {
        return invalidPatternRuleCount;
    }

    public void setInvalidPatternRuleCount(long invalidPatternRuleCount) {
        this.invalidPatternRuleCount = invalidPatternRuleCount;
    }

    public List<RuleRiskEntryDto> getEntries() {
        return entries;
    }

    public void setEntries(List<RuleRiskEntryDto> entries) {
        this.entries = entries == null ? new ArrayList<>() : entries;
    }

    public List<Map<String, Object>> getDuplicateGroups() {
        return duplicateGroups;
    }

    public void setDuplicateGroups(List<Map<String, Object>> duplicateGroups) {
        this.duplicateGroups = duplicateGroups == null ? new ArrayList<>() : duplicateGroups;
    }

    public List<RuleRiskRemediationItemDto> getRemediation() {
        return remediation;
    }

    public void setRemediation(List<RuleRiskRemediationItemDto> remediation) {
        this.remediation = remediation == null ? new ArrayList<>() : remediation;
    }

    public void addDuplicateGroup(String normalizedPattern, int ruleCount, List<String> categories, List<String> ruleIds) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("normalizedPattern", normalizedPattern);
        row.put("ruleCount", ruleCount);
        row.put("categories", categories);
        row.put("ruleIds", ruleIds);
        duplicateGroups.add(row);
    }
}

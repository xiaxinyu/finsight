package com.finsight.application.classification;

import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.application.consume.ConsumeRuleService;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.model.ConsumeRule;
import com.finsight.web.api.dto.RuleRiskEntryDto;
import com.finsight.web.api.dto.RuleRiskRemediationItemDto;
import com.finsight.web.api.dto.RuleRiskReportDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RuleRiskAnalysisService {

    private final ConsumeRuleService ruleService;
    private final ConsumeCategoryService categoryService;

    public RuleRiskAnalysisService(ConsumeRuleService ruleService,
                                   ConsumeCategoryService categoryService) {
        this.ruleService = ruleService;
        this.categoryService = categoryService;
    }

    public RuleRiskReportDto analyze() {
        List<ConsumeCategory> allCategories = categoryService.listAll();
        List<ConsumeCategory> activeCategories = allCategories.stream()
                .filter(c -> c != null && (c.getDeleted() == null || c.getDeleted() != 1))
                .collect(Collectors.toList());
        Set<String> activeCategoryIds = OrphanRuleSupport.activeCategoryIds(activeCategories);
        Set<String> activeCodes = OrphanRuleSupport.activeCategoryCodes(activeCategories);
        Map<String, ConsumeCategory> categoryByRef = RuleRiskSupport.indexCategoriesByRef(allCategories);

        List<ConsumeRule> rules = ruleService.list();
        Map<String, List<ConsumeRule>> patternGroups = RuleRiskSupport.groupActiveByNormalizedPattern(rules);

        RuleRiskReportDto report = new RuleRiskReportDto();
        long activeCount = 0;
        long highRisk = 0;
        long crossCategory = 0;
        long broad = 0;
        long direction = 0;
        long orphan = 0;
        long invalid = 0;

        for (Map.Entry<String, List<ConsumeRule>> group : patternGroups.entrySet()) {
            if (group.getValue().size() > 1) {
                List<String> categories = group.getValue().stream()
                        .map(ConsumeRule::getCategoryId)
                        .filter(StringUtils::isNotBlank)
                        .map(String::trim)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());
                List<String> ruleIds = group.getValue().stream()
                        .map(ConsumeRule::getId)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toList());
                report.addDuplicateGroup(group.getKey(), group.getValue().size(), categories, ruleIds);
            }
        }
        report.setDuplicatePatternGroupCount(report.getDuplicateGroups().size());

        for (ConsumeRule rule : rules) {
            if (RuleRiskSupport.isAnalyzableActiveRule(rule)) {
                activeCount++;
            }
            Set<RuleRiskKind> risks = RuleRiskSupport.analyzeRule(
                    rule, categoryByRef, activeCategoryIds, activeCodes, patternGroups);
            if (risks.isEmpty()) {
                continue;
            }

            RuleRiskEntryDto entry = toEntry(rule, risks, patternGroups);
            report.getEntries().add(entry);

            if (entry.isHighRisk()) {
                highRisk++;
            }
            if (risks.contains(RuleRiskKind.CROSS_CATEGORY_CONFLICT)) {
                crossCategory++;
            }
            if (risks.contains(RuleRiskKind.BROAD_KEYWORD)) {
                broad++;
            }
            if (risks.contains(RuleRiskKind.DIRECTION_MISMATCH)) {
                direction++;
            }
            if (risks.contains(RuleRiskKind.ORPHAN_CATEGORY)) {
                orphan++;
            }
            if (risks.contains(RuleRiskKind.INVALID_PATTERN)) {
                invalid++;
            }

            RuleRiskRemediationItemDto item = new RuleRiskRemediationItemDto();
            item.setRuleId(rule.getId());
            item.setPattern(rule.getPattern());
            item.setCategoryId(rule.getCategoryId());
            item.setPriority(rule.getPriority());
            item.setRisks(riskNames(risks));
            item.setSuggestedAction(buildCombinedSuggestion(risks));
            report.getRemediation().add(item);
        }

        report.getEntries().sort(Comparator
                .comparing(RuleRiskEntryDto::isHighRisk).reversed()
                .thenComparing(e -> e.getRisks() == null ? 0 : e.getRisks().size(), Comparator.reverseOrder())
                .thenComparing(RuleRiskEntryDto::getPattern, Comparator.nullsLast(String::compareToIgnoreCase)));

        report.getRemediation().sort(Comparator
                .comparing((RuleRiskRemediationItemDto i) -> i.getRisks() == null ? 0 : i.getRisks().size())
                .reversed()
                .thenComparing(RuleRiskRemediationItemDto::getPattern, Comparator.nullsLast(String::compareToIgnoreCase)));

        report.setActiveRuleCount(activeCount);
        report.setHighRiskRuleCount(highRisk);
        report.setCrossCategoryConflictRuleCount(crossCategory);
        report.setBroadKeywordRuleCount(broad);
        report.setDirectionMismatchRuleCount(direction);
        report.setOrphanRuleCount(orphan);
        report.setInvalidPatternRuleCount(invalid);
        return report;
    }

    private static RuleRiskEntryDto toEntry(
            ConsumeRule rule,
            Set<RuleRiskKind> risks,
            Map<String, List<ConsumeRule>> patternGroups) {
        RuleRiskEntryDto entry = new RuleRiskEntryDto();
        entry.setRuleId(rule.getId());
        entry.setPattern(rule.getPattern());
        entry.setCategoryId(rule.getCategoryId());
        entry.setPriority(rule.getPriority());
        entry.setActive(rule.getActive());
        entry.setRisks(riskNames(risks));
        entry.setHighRisk(RuleRiskSupport.isHighRisk(risks));
        entry.setSuggestion(buildCombinedSuggestion(risks));

        if (RuleRiskSupport.isAnalyzableActiveRule(rule)) {
            String key = RuleRiskSupport.normalizePattern(rule.getPattern());
            entry.setDuplicateGroupKey(key);
            List<ConsumeRule> peers = patternGroups.get(key);
            if (peers != null && peers.size() > 1) {
                for (ConsumeRule peer : peers) {
                    if (!StringUtils.equals(peer.getId(), rule.getId())) {
                        entry.getDuplicatePeerRuleIds().add(peer.getId());
                    }
                    if (StringUtils.isNotBlank(peer.getCategoryId())) {
                        entry.getDuplicatePeerCategoryIds().add(peer.getCategoryId().trim());
                    }
                }
                entry.setDuplicatePeerCategoryIds(entry.getDuplicatePeerCategoryIds().stream()
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList()));
            }
        }
        return entry;
    }

    private static List<String> riskNames(Set<RuleRiskKind> risks) {
        List<String> names = new ArrayList<>();
        for (RuleRiskKind kind : risks) {
            names.add(kind.name());
        }
        return names;
    }

    private static String buildCombinedSuggestion(Set<RuleRiskKind> risks) {
        Set<String> parts = new LinkedHashSet<>();
        for (RuleRiskKind kind : risks) {
            parts.add(RuleRiskSupport.suggestAction(kind));
        }
        return String.join(" · ", parts);
    }
}

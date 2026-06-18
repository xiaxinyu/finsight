package com.finsight.application.classification;

import com.finsight.application.consume.ClassificationNarrationBuilder;
import com.finsight.application.consume.ClassificationService;
import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.application.consume.ConsumeRuleService;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.model.ConsumeRule;
import com.finsight.domain.model.Page;
import com.finsight.domain.model.Transaction;
import com.finsight.application.query.TransactionQuery;
import com.finsight.domain.port.TransactionRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ClassificationRuleHygieneService {

    private final ConsumeRuleService ruleService;
    private final ConsumeCategoryService categoryService;
    private final TransactionRepository transactionRepository;
    private final ClassificationService classificationService;

    public ClassificationRuleHygieneService(ConsumeRuleService ruleService,
                                              ConsumeCategoryService categoryService,
                                              TransactionRepository transactionRepository,
                                              ClassificationService classificationService) {
        this.ruleService = ruleService;
        this.categoryService = categoryService;
        this.transactionRepository = transactionRepository;
        this.classificationService = classificationService;
    }

    public List<ConsumeRule> listOrphanRules() {
        List<ConsumeCategory> activeCategories = activeCategories();
        Set<String> activeCategoryIds = OrphanRuleSupport.activeCategoryIds(activeCategories);
        Set<String> activeCodes = OrphanRuleSupport.activeCategoryCodes(activeCategories);

        List<ConsumeRule> orphans = new ArrayList<>();
        for (ConsumeRule rule : ruleService.list()) {
            if (OrphanRuleSupport.isActiveOrphan(rule, activeCategoryIds, activeCodes)) {
                orphans.add(rule);
            }
        }
        ruleService.loadTags(orphans);
        return orphans;
    }

    public List<ConsumeRule> listArchivedLegacyOrphanRules() {
        List<ConsumeCategory> activeCategories = activeCategories();
        Set<String> activeCategoryIds = OrphanRuleSupport.activeCategoryIds(activeCategories);
        Set<String> activeCodes = OrphanRuleSupport.activeCategoryCodes(activeCategories);

        List<ConsumeRule> archived = new ArrayList<>();
        for (ConsumeRule rule : ruleService.list()) {
            if (OrphanRuleSupport.isLegacyArchived(rule)
                    && !OrphanRuleSupport.pointsToActiveCategory(rule, activeCategoryIds, activeCodes)) {
                archived.add(rule);
            }
        }
        ruleService.loadTags(archived);
        return archived;
    }

    private List<ConsumeCategory> activeCategories() {
        return categoryService.listAll().stream()
                .filter(c -> c != null && (c.getDeleted() == null || c.getDeleted() != 1))
                .collect(Collectors.toList());
    }

    public List<String> recommendKeywordsFromUnclassified(int limit) {
        int cap = limit <= 0 ? 20 : Math.min(limit, 50);
        TransactionQuery q = new TransactionQuery();
        q.setEmptyConsume(Boolean.TRUE);
        Page page = new Page(1, 500);
        List<Transaction> list = transactionRepository.getTransactions(q, page);
        Map<String, Integer> freq = new HashMap<>();
        for (Transaction t : list) {
            if (t == null) {
                continue;
            }
            String narration = ClassificationNarrationBuilder.fromTransaction(t);
            List<String> tokens = classificationService.tokens(narration);
            if (tokens == null) {
                continue;
            }
            for (String token : tokens) {
                String k = StringUtils.trimToEmpty(token);
                if (k.length() <= 1) {
                    continue;
                }
                freq.merge(k, 1, Integer::sum);
            }
        }
        return freq.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, Integer> e) -> e.getValue()).reversed())
                .limit(cap)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public Map<String, Object> hygieneSummary() {
        List<ConsumeRule> orphans = listOrphanRules();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("orphanCount", orphans.size());
        out.put("archivedLegacyOrphanCount", listArchivedLegacyOrphanRules().size());
        out.put("recommendedKeywords", recommendKeywordsFromUnclassified(15));
        return out;
    }
}

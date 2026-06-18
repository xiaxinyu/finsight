package com.finsight.application.classification;

import com.finsight.application.consume.ClassificationService;
import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.application.consume.ConsumeRuleService;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.model.ConsumeRule;
import com.finsight.domain.port.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassificationRuleHygieneServiceTest {

    @Mock
    private ConsumeRuleService ruleService;
    @Mock
    private ConsumeCategoryService categoryService;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ClassificationService classificationService;

    private ClassificationRuleHygieneService service;

    @BeforeEach
    void setUp() {
        service = new ClassificationRuleHygieneService(
                ruleService, categoryService, transactionRepository, classificationService);
    }

    @Test
    void listOrphanRules_returnsOnlyActiveOrphans() {
        ConsumeCategory daily = category("c1", "DAILY-01", 0);
        when(categoryService.listAll()).thenReturn(List.of(daily));

        ConsumeRule activeOrphan = rule("r1", "GONE", 1, null);
        ConsumeRule archivedOrphan = rule("r2", "GONE", 0, OrphanRuleSupport.LEGACY_ORPHAN_REMARK);
        ConsumeRule healthy = rule("r3", "DAILY-01", 1, null);
        when(ruleService.list()).thenReturn(List.of(activeOrphan, archivedOrphan, healthy));

        List<ConsumeRule> orphans = service.listOrphanRules();

        assertEquals(1, orphans.size());
        assertEquals("r1", orphans.get(0).getId());
        verify(ruleService).loadTags(orphans);
    }

    @Test
    void hygieneSummary_countsActiveOrphansOnly() {
        ConsumeCategory daily = category("c1", "DAILY-01", 0);
        when(categoryService.listAll()).thenReturn(List.of(daily));
        when(ruleService.list()).thenReturn(List.of(
                rule("r1", "GONE", 1, null),
                rule("r2", "GONE", 0, OrphanRuleSupport.AUTO_DISABLED_ORPHAN_REMARK)));
        when(transactionRepository.getTransactions(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        Map<String, Object> summary = service.hygieneSummary();

        assertEquals(1, summary.get("orphanCount"));
        assertEquals(1, summary.get("archivedLegacyOrphanCount"));
    }

    private static ConsumeCategory category(String id, String code, int deleted) {
        ConsumeCategory c = new ConsumeCategory();
        c.setId(id);
        c.setCode(code);
        c.setDeleted(deleted);
        return c;
    }

    private static ConsumeRule rule(String id, String categoryId, int active, String remark) {
        ConsumeRule r = new ConsumeRule();
        r.setId(id);
        r.setCategoryId(categoryId);
        r.setActive(active);
        r.setRemark(remark);
        r.setPattern("test");
        return r;
    }
}

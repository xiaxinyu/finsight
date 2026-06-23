package com.finsight.application.classification;

import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.application.consume.ConsumeRuleService;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.model.ConsumeRule;
import com.finsight.web.api.dto.RuleRiskEntryDto;
import com.finsight.web.api.dto.RuleRiskReportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleRiskAnalysisServiceTest {

    @Mock
    private ConsumeRuleService ruleService;
    @Mock
    private ConsumeCategoryService categoryService;

    private RuleRiskAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new RuleRiskAnalysisService(ruleService, categoryService);
    }

    @Test
    void analyze_reportsDuplicateGroupsAndHighRiskCounts() {
        ConsumeCategory daily = category("c1", "DAILY-01", "expense");
        ConsumeCategory transport = category("c2", "TRANSPORT", "expense");
        when(categoryService.listAll()).thenReturn(List.of(daily, transport));

        ConsumeRule r1 = rule("r1", "支付", "DAILY-01", 10);
        ConsumeRule r2 = rule("r2", "支付", "TRANSPORT", 20);
        ConsumeRule healthy = rule("r3", "地铁", "DAILY-01", 5);
        when(ruleService.list()).thenReturn(List.of(r1, r2, healthy));

        RuleRiskReportDto report = service.analyze();

        assertEquals(1, report.getDuplicatePatternGroupCount());
        assertEquals(3, report.getActiveRuleCount());
        assertTrue(report.getHighRiskRuleCount() >= 2);
        assertTrue(report.getCrossCategoryConflictRuleCount() >= 2);
        assertTrue(report.getBroadKeywordRuleCount() >= 2);
        assertEquals(1, report.getDuplicateGroups().size());
        assertTrue(report.getRemediation().size() >= 2);

        RuleRiskEntryDto first = report.getEntries().stream()
                .filter(e -> "r1".equals(e.getRuleId()))
                .findFirst()
                .orElseThrow();
        assertTrue(first.isHighRisk());
        assertTrue(first.getRisks().contains("DUPLICATE_PATTERN"));
        assertTrue(first.getDuplicatePeerRuleIds().contains("r2"));
    }

    private static ConsumeCategory category(String id, String code, String txnTypes) {
        ConsumeCategory c = new ConsumeCategory();
        c.setId(id);
        c.setCode(code);
        c.setTxnTypes(txnTypes);
        c.setDeleted(0);
        return c;
    }

    private static ConsumeRule rule(String id, String pattern, String categoryId, int priority) {
        ConsumeRule r = new ConsumeRule();
        r.setId(id);
        r.setPattern(pattern);
        r.setCategoryId(categoryId);
        r.setPriority(priority);
        r.setActive(1);
        return r;
    }
}

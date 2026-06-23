package com.finsight.application.classification;

import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.application.consume.ConsumeRuleService;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.web.api.dto.CategoryImpactPreview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryImpactPreviewServiceTest {

    @Mock
    private ConsumeCategoryService categoryService;
    @Mock
    private ConsumeRuleService ruleService;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private CategoryImpactPreviewService service;

    @BeforeEach
    void setUp() {
        service = new CategoryImpactPreviewService(categoryService, ruleService, jdbcTemplate);
    }

    @Test
    void previewDeleteAggregatesCounts() {
        ConsumeCategory cat = category("DAILY-01", "Daily food");
        when(categoryService.getById("DAILY-01")).thenReturn(cat);
        when(categoryService.count(any())).thenReturn(0L);
        when(ruleService.count(any())).thenReturn(2L, 1L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(10L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), any(Object[].class))).thenReturn(1234.5);
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                Map.of("year_month", "2026-01", "txn_count", 4, "amount", 200.0)));

        CategoryImpactPreview preview = service.preview("DAILY-01", CategoryImpactAction.DELETE, null);

        assertEquals("DAILY-01", preview.getCategoryCode());
        assertEquals(10, preview.getTransactionCount());
        assertEquals(1234.5, preview.getTotalAmount());
        assertEquals(2, preview.getActiveRuleCount());
        assertEquals(1, preview.getAmountByMonth().size());
        assertTrue(preview.getAffectedReports().size() >= 5);
        assertTrue(preview.getSummary().contains("Deleting"));
    }

    private static ConsumeCategory category(String code, String name) {
        ConsumeCategory cat = new ConsumeCategory();
        cat.setId(code);
        cat.setCode(code);
        cat.setName(name);
        cat.setDeleted(0);
        return cat;
    }
}

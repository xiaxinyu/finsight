package com.finsight.application.classification;

import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.application.consume.ConsumeRuleService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.model.ConsumeRule;
import com.finsight.web.api.dto.CategoryAssetDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryAssetServiceTest {

    @Mock
    private ConsumeCategoryService categoryService;
    @Mock
    private ConsumeRuleService ruleService;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private CategoryAssetService service;

    @BeforeEach
    void setUp() {
        service = new CategoryAssetService(categoryService, ruleService, jdbcTemplate);
    }

    @Test
    void loadAssetAggregatesUsageAndRules() {
        ConsumeCategory cat = l1("LIVING", "日常生活");
        when(categoryService.getById("LIVING")).thenReturn(cat);
        when(categoryService.list(any(Wrapper.class))).thenReturn(List.of(cat));
        when(categoryService.count(any())).thenReturn(2L);
        when(ruleService.count(any())).thenReturn(3L, 1L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(42L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Double.class), any(Object[].class))).thenReturn(900.0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Date.class), any(Object[].class)))
                .thenReturn(Date.valueOf("2026-06-01"));
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                Map.of("txn_month", "2026-06", "txn_count", 5, "amount", 100.0)));

        CategoryAssetDto asset = service.loadAsset("LIVING");

        assertEquals("LIVING", asset.getCategoryCode());
        assertEquals(42, asset.getTransactionCount());
        assertEquals(900.0, asset.getTotalAmount());
        assertEquals("2026-06-01", asset.getLastTransactionDate());
        assertEquals(3, asset.getActiveRuleCount());
        assertEquals(1, asset.getInactiveRuleCount());
        assertEquals(2, asset.getChildCategoryCount());
        assertTrue(asset.getAffectedReports().size() >= 5);
        assertFalse(asset.getQualityFlags().contains("empty"));
    }

    @Test
    void loadSummaryByCodeRollsUpL1Children() {
        ConsumeCategory l1 = l1("LIVING", "日常生活");
        ConsumeCategory l2 = category("DAILY-01", "餐饮", 2, "LIVING");
        when(categoryService.list(any(Wrapper.class))).thenReturn(List.of(l1, l2));
        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(
                Map.of("code", "DAILY-01", "cnt", 10L)));
        ConsumeRule rule = new ConsumeRule();
        rule.setCategoryId("DAILY-01");
        rule.setActive(1);
        when(ruleService.list()).thenReturn(List.of(rule));

        var summary = service.loadSummaryByCode();

        assertEquals(10, summary.get("LIVING").getTransactionCount());
        assertEquals(1, summary.get("LIVING").getActiveRuleCount());
    }

    private static ConsumeCategory l1(String code, String name) {
        return category(code, name, 1, null);
    }

    private static ConsumeCategory category(String code, String name, int level, String parentId) {
        ConsumeCategory cat = new ConsumeCategory();
        cat.setId(code);
        cat.setCode(code);
        cat.setName(name);
        cat.setLevel(level);
        cat.setParentId(parentId);
        cat.setDeleted(0);
        return cat;
    }
}

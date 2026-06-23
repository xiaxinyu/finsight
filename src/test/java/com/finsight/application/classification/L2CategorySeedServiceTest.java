package com.finsight.application.classification;

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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class L2CategorySeedServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private L2CategorySeedService service;

    @BeforeEach
    void setUp() {
        service = new L2CategorySeedService(jdbcTemplate);
    }

    @Test
    void buildSeedPlanSummarizesInsertActions() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("cls_category")))
                .thenReturn(1);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(List.of("OTHER-01", "TRAVEL-01"));

        Map<String, Object> plan = service.buildSeedPlan();

        assertEquals(2, plan.get("existingCodeCount"));
        assertTrue((Long) plan.get("insertCount") > 0);
        assertEquals("docs/tech/database/l2-category-sprint2-seed.sql", plan.get("manualScript"));
    }

    @Test
    void buildSeedPlanEmptyWhenTableMissing() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("cls_category")))
                .thenReturn(0);

        Map<String, Object> plan = service.buildSeedPlan();

        assertEquals(0, plan.get("existingCodeCount"));
        assertEquals((long) ClassificationL2TargetCatalog.insertableBatch().size(), plan.get("insertCount"));
    }
}

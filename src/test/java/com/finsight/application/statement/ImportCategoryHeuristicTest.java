package com.finsight.application.statement;

import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.domain.model.ConsumeCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ImportCategoryHeuristicTest {

    @Mock
    private ConsumeCategoryService categoryService;

    private ImportCategoryHeuristic heuristic;

    @BeforeEach
    void setUp() {
        heuristic = new ImportCategoryHeuristic(categoryService);
        lenient().when(categoryService.listAll()).thenReturn(List.of(
                category("TRAVEL-01", "公共交通", "TRAVEL"),
                category("FOOD-01", "餐饮", "FOOD"),
                category("INVEST-01", "基金申购（买入基金）", "INVEST"),
                category("LOAN-01", "贷款还款（消费贷、房贷、信用卡）", "LOAN")
        ));
    }

    @Test
    void matchesMetroMerchantToTravelCategory() {
        Optional<ImportCategoryHeuristic.Match> match =
                heuristic.match("深圳市地铁相关运营主体", 6);
        assertTrue(match.isPresent());
        assertEquals("TRAVEL-01", match.get().categoryCode());
        assertEquals(ImportCategoryHeuristic.Family.TRANSIT, match.get().family());
    }

    @Test
    void overridesInvestRuleWhenMetroPresent() {
        assertTrue(heuristic.shouldOverrideRule(
                "INVEST-01",
                "基金申购（买入基金）",
                new ImportCategoryHeuristic.Match(
                        ImportCategoryHeuristic.Family.TRANSIT,
                        "TRAVEL-01",
                        "公共交通",
                        "test"),
                "深圳市地铁相关运营主体"));
    }

    @Test
    void doesNotOverrideInvestWhenFundKeywordsPresent() {
        assertTrue(heuristic.match("天天基金申购确认", 1000).isPresent());
        assertFalse(heuristic.shouldOverrideRule(
                "INVEST-01",
                "基金申购（买入基金）",
                new ImportCategoryHeuristic.Match(
                        ImportCategoryHeuristic.Family.TRANSIT,
                        "TRAVEL-01",
                        "公共交通",
                        "test"),
                "天天基金申购确认"));
    }

    @Test
    void matchesInstallmentDescription() {
        Optional<ImportCategoryHeuristic.Match> match =
                heuristic.match("分期17/36:账单分期单期金额", 1331.25);
        assertTrue(match.isPresent());
        assertEquals("LOAN-01", match.get().categoryCode());
    }

    private static ConsumeCategory category(String code, String name, String parent) {
        ConsumeCategory c = new ConsumeCategory();
        c.setCode(code);
        c.setName(name);
        c.setParentId(parent);
        c.setDeleted(0);
        return c;
    }
}

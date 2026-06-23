package com.finsight.application.classification;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryReportRoleInferenceTest {

    @Test
    void infersFromCatalogWhenCodeMatches() {
        Optional<String> role = CategoryReportRoleInference.inferReportRole(
                new CategoryReportRoleInference.DbCategoryRow("INC-01", "工资", 2, "INC", "income"));
        assertEquals("income", role.orElseThrow());
    }

    @Test
    void infersLivingSubcategoryAsBudget() {
        Optional<String> role = CategoryReportRoleInference.inferReportRole(
                new CategoryReportRoleInference.DbCategoryRow("LIVING-06", "基础医疗", 2, "LIVING", "expense"));
        assertEquals("budget", role.orElseThrow());
    }

    @Test
    void infersLegacyFeeParentAsCashflow() {
        Optional<String> role = CategoryReportRoleInference.inferReportRole(
                new CategoryReportRoleInference.DbCategoryRow("FE-01", "提现费", 2, "FE", "expense"));
        assertEquals("cashflow", role.orElseThrow());
    }

    @Test
    void infersReimbUnderLegacyParent() {
        Optional<String> role = CategoryReportRoleInference.inferReportRole(
                new CategoryReportRoleInference.DbCategoryRow("REIMB-01", "餐补", 2, "REIMB", "income,refund"));
        assertEquals("refund", role.orElseThrow());
    }

    @Test
    void rendererIncludesDbInferredRoles() {
        String sql = L2CategorySeedSqlRenderer.renderReportRoleBackfillFromDatabase(java.util.List.of(
                new CategoryReportRoleInference.DbCategoryRow("LIVING-06", "基础医疗", 2, "LIVING", "expense"),
                new CategoryReportRoleInference.DbCategoryRow("FE-01", "提现费", 2, "FE", "expense")));
        assertTrue(sql.contains("LIVING-06"));
        assertTrue(sql.contains("FE-01"));
        assertTrue(sql.contains("report_role is null"));
    }
}

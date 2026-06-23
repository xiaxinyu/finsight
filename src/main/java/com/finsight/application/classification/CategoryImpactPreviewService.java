package com.finsight.application.classification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.application.consume.ConsumeRuleService;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.model.ConsumeRule;
import com.finsight.web.api.dto.CategoryImpactPreview;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class CategoryImpactPreviewService {

    private static final int MONTHLY_LIMIT = 24;

    private final ConsumeCategoryService categoryService;
    private final ConsumeRuleService ruleService;
    private final JdbcTemplate jdbcTemplate;

    public CategoryImpactPreviewService(ConsumeCategoryService categoryService,
                                        ConsumeRuleService ruleService,
                                        JdbcTemplate jdbcTemplate) {
        this.categoryService = categoryService;
        this.ruleService = ruleService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public CategoryImpactPreview preview(String categoryId, CategoryImpactAction action, String targetCode) {
        ConsumeCategory cat = categoryService.getById(categoryId);
        if (cat == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: " + categoryId);
        }
        List<String> refs = CategoryImpactSupport.categoryRefs(cat);
        long txnCount = countTransactions(refs);
        double totalAmount = sumTransactionAmount(refs);
        long activeRules = countRules(cat, true);
        long inactiveRules = countRules(cat, false);
        long childCount = countChildCategories(cat);

        CategoryImpactPreview out = new CategoryImpactPreview();
        out.setCategoryId(cat.getId());
        out.setCategoryCode(cat.getCode());
        out.setCategoryName(cat.getName());
        out.setAction(action.name().toLowerCase());
        out.setTransactionCount(txnCount);
        out.setTotalAmount(totalAmount);
        out.setActiveRuleCount(activeRules);
        out.setInactiveRuleCount(inactiveRules);
        out.setChildCategoryCount(childCount);
        for (Map<String, Object> row : loadAmountByMonth(refs)) {
            out.addMonth(
                    String.valueOf(row.get("txn_month")),
                    row.get("txn_count") == null ? 0 : ((Number) row.get("txn_count")).longValue(),
                    row.get("amount") == null ? 0 : ((Number) row.get("amount")).doubleValue());
        }
        out.setAffectedReports(CategoryImpactSupport.REPORT_SURFACES);
        out.setWarnings(CategoryImpactSupport.warningsFor(action, childCount, txnCount, activeRules, targetCode));
        out.setSummary(buildSummary(action, cat, txnCount, totalAmount, activeRules, targetCode));

        if (action == CategoryImpactAction.MERGE && StringUtils.isNotBlank(targetCode)) {
            ConsumeCategory target = categoryService.getOne(
                    Wrappers.<ConsumeCategory>lambdaQuery().eq(ConsumeCategory::getCode, targetCode.trim()),
                    false);
            if (target == null) {
                out.getWarnings().add("Target category not found: " + targetCode.trim());
            } else {
                out.setTargetCode(target.getCode());
                out.setTargetName(target.getName());
                if (CategoryMergeSupport.isLevelOne(cat) && CategoryMergeSupport.isLevelOne(target)) {
                    out.getWarnings().removeIf(w -> w.startsWith("Category has"));
                    if (childCount > 0) {
                        out.getWarnings().add(childCount + " child categories will be reparented under "
                                + target.getCode() + " (duplicate L1 merge).");
                    }
                }
            }
        }
        return out;
    }

    private long countTransactions(List<String> refs) {
        if (refs.isEmpty()) {
            return 0;
        }
        String sql = "select count(*) from `transaction` t where coalesce(t.deleted,0)=0 and "
                + CategoryImpactSupport.transactionMatchSql(refs);
        Long count = jdbcTemplate.queryForObject(sql, Long.class, CategoryImpactSupport.transactionMatchParams(refs));
        return count == null ? 0 : count;
    }

    private double sumTransactionAmount(List<String> refs) {
        if (refs.isEmpty()) {
            return 0;
        }
        String sql = "select coalesce(round(sum("
                + "abs(coalesce(t.expense_amount,0)) + abs(coalesce(t.income_money,0))"
                + "), 2), 0) from `transaction` t where coalesce(t.deleted,0)=0 and "
                + CategoryImpactSupport.transactionMatchSql(refs);
        Double total = jdbcTemplate.queryForObject(sql, Double.class, CategoryImpactSupport.transactionMatchParams(refs));
        return total == null ? 0 : total;
    }

    private List<Map<String, Object>> loadAmountByMonth(List<String> refs) {
        if (refs.isEmpty()) {
            return List.of();
        }
        String sql = CategoryImpactSupport.monthlyAmountSql(refs, MONTHLY_LIMIT);
        return jdbcTemplate.queryForList(sql, CategoryImpactSupport.transactionMatchParams(refs));
    }

    private long countChildCategories(ConsumeCategory cat) {
        if (cat == null || StringUtils.isBlank(cat.getCode())) {
            return 0;
        }
        LambdaQueryWrapper<ConsumeCategory> qw = Wrappers.lambdaQuery();
        qw.eq(ConsumeCategory::getParentId, cat.getCode())
                .ne(ConsumeCategory::getDeleted, 1);
        return categoryService.count(qw);
    }

    private long countRules(ConsumeCategory cat, boolean active) {
        if (cat == null) {
            return 0;
        }
        String code = StringUtils.trimToNull(cat.getCode());
        String id = StringUtils.trimToNull(cat.getId());
        if (code == null && id == null) {
            return 0;
        }
        LambdaQueryWrapper<ConsumeRule> qw = Wrappers.lambdaQuery();
        qw.and(w -> {
            if (code != null) {
                w.eq(ConsumeRule::getCategoryId, code);
            }
            if (id != null) {
                if (code != null) {
                    w.or().eq(ConsumeRule::getCategoryId, id);
                } else {
                    w.eq(ConsumeRule::getCategoryId, id);
                }
            }
        });
        if (active) {
            qw.and(w -> w.eq(ConsumeRule::getActive, 1).or().isNull(ConsumeRule::getActive));
        } else {
            qw.eq(ConsumeRule::getActive, 0);
        }
        return ruleService.count(qw);
    }

    private static String buildSummary(CategoryImpactAction action,
                                       ConsumeCategory cat,
                                       long txnCount,
                                       double totalAmount,
                                       long activeRules,
                                       String targetCode) {
        String label = StringUtils.defaultString(cat.getName(), cat.getCode());
        return switch (action) {
            case DELETE -> String.format(
                    "Deleting \"%s\" affects %d transactions (%.2f total), %d active rules, and %d report surfaces.",
                    label, txnCount, totalAmount, activeRules, CategoryImpactSupport.REPORT_SURFACES.size());
            case RENAME -> String.format(
                    "Renaming \"%s\" does not change consume_code; %d transactions and %d rules reference this code.",
                    label, txnCount, activeRules);
            case MERGE -> String.format(
                    "Merging \"%s\" into \"%s\" affects %d transactions and %d active rules (category codes are preserved; rules remap to target).",
                    label, StringUtils.defaultString(targetCode, "?"), txnCount, activeRules);
        };
    }
}

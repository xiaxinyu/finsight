package com.finsight.application.classification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.application.consume.ConsumeRuleService;
import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.model.ConsumeRule;
import com.finsight.web.api.dto.CategoryAssetDto;
import com.finsight.web.api.dto.CategoryAssetSummaryRow;
import com.finsight.web.api.dto.CategoryChildCandidateDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CategoryAssetService {

    private static final int MONTHLY_LIMIT = 24;

    private final ConsumeCategoryService categoryService;
    private final ConsumeRuleService ruleService;
    private final JdbcTemplate jdbcTemplate;

    public CategoryAssetService(ConsumeCategoryService categoryService,
                                ConsumeRuleService ruleService,
                                JdbcTemplate jdbcTemplate) {
        this.categoryService = categoryService;
        this.ruleService = ruleService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public CategoryAssetDto loadAsset(String categoryId) {
        ConsumeCategory cat = categoryService.getById(categoryId);
        if (cat == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: " + categoryId);
        }
        List<String> refs = CategoryImpactSupport.categoryRefs(cat);
        List<ConsumeCategory> activeCategories = listActiveCategories();
        Set<String> activeIds = OrphanRuleSupport.activeCategoryIds(activeCategories);
        Set<String> activeCodes = OrphanRuleSupport.activeCategoryCodes(activeCategories);

        CategoryAssetDto out = new CategoryAssetDto();
        out.setCategoryId(cat.getId());
        out.setCategoryCode(cat.getCode());
        out.setCategoryName(cat.getName());
        out.setLevel(cat.getLevel());
        out.setParentId(cat.getParentId());
        out.setTransactionCount(countTransactions(refs));
        out.setTotalAmount(sumTransactionAmount(refs));
        out.setLastTransactionDate(formatLastDate(loadLastTransactionDate(refs)));
        out.setActiveRuleCount(countRules(cat, true));
        out.setInactiveRuleCount(countRules(cat, false));
        out.setOrphanRuleCount(countOrphanRulesForCategory(cat, activeIds, activeCodes));
        out.setChildCategoryCount(countChildCategories(cat));
        for (Map<String, Object> row : loadAmountByMonth(refs)) {
            out.addMonth(
                    String.valueOf(row.get("txn_month")),
                    row.get("txn_count") == null ? 0 : ((Number) row.get("txn_count")).longValue(),
                    row.get("amount") == null ? 0 : ((Number) row.get("amount")).doubleValue());
        }
        out.setAffectedReports(CategoryImpactSupport.REPORT_SURFACES);
        out.setQualityFlags(buildQualityFlags(out));
        applyFinanceSemantics(out, cat);
        if (CategoryMergeSupport.isLevelOne(cat)) {
            out.setChildCandidates(listChildCandidates(cat, loadAllCategoryCodes()));
        }
        return out;
    }

    public Map<String, CategoryAssetSummaryRow> loadSummaryByCode() {
        Map<String, Long> txnByCode = loadTransactionCountsByCode();
        Map<String, Long> activeRulesByRef = loadActiveRuleCountsByCategoryRef();
        Map<String, CategoryAssetSummaryRow> out = new LinkedHashMap<>();
        for (ConsumeCategory cat : listActiveCategories()) {
            String code = StringUtils.trimToNull(cat.getCode());
            if (code == null) {
                continue;
            }
            long txnCount = aggregateTxnCountForCategory(cat, txnByCode);
            long activeRules = aggregateActiveRulesForCategory(cat, activeRulesByRef);
            out.put(code, new CategoryAssetSummaryRow(code, txnCount, activeRules));
        }
        return out;
    }

    private List<CategoryChildCandidateDto> listChildCandidates(ConsumeCategory l1, Set<String> existingCodes) {
        if (l1 == null || StringUtils.isBlank(l1.getCode())) {
            return List.of();
        }
        String parentCode = ClassificationL1Codes.resolveParentL1(l1.getCode(), existingCodes);
        List<CategoryChildCandidateDto> candidates = new ArrayList<>();
        for (L2CategorySeedPlanner.SeedItem item : L2CategorySeedPlanner.buildInsertPlan(existingCodes)) {
            if (item.action() != L2CategorySeedPlanner.Action.INSERT) {
                continue;
            }
            if (!parentCode.equals(item.parentL1Code())) {
                continue;
            }
            CategoryChildCandidateDto dto = new CategoryChildCandidateDto();
            dto.setCode(item.code());
            dto.setName(item.name());
            dto.setParentL1Code(item.parentL1Code());
            dto.setSortNo(item.sortNo());
            dto.setTxnTypes(item.txnTypes());
            dto.setReportRole(item.reportRole());
            dto.setReason(item.reason());
            candidates.add(dto);
        }
        return candidates;
    }

    private static void applyFinanceSemantics(CategoryAssetDto out, ConsumeCategory cat) {
        String role = StringUtils.trimToNull(cat.getReportRole());
        if (role == null) {
            role = CategoryReportRoleInference.inferReportRole(
                    new CategoryReportRoleInference.DbCategoryRow(
                            cat.getCode(),
                            cat.getName(),
                            cat.getLevel() == null ? 0 : cat.getLevel(),
                            cat.getParentId(),
                            cat.getTxnTypes()))
                    .orElse("other");
        }
        CategoryFinanceSemantics.SemanticProfile sem = CategoryFinanceSemantics.profile(
                role, cat.getTxnTypes(), cat.getParentId(), cat.getCode());
        out.setReportRole(sem.reportRole());
        out.setSemanticTag(StringUtils.trimToNull(cat.getSemanticTag()));
        out.setTxnTypes(cat.getTxnTypes());
        out.setEconomicNature(sem.economicNature());
        out.setBudgetBehavior(sem.budgetBehavior());
        out.setFixedCostKind(sem.fixedCostKind());
        out.setIncludeInIncomeTrend(sem.includeInIncomeTrend());
        out.setIncludeInExpenseTrend(sem.includeInExpenseTrend());
        out.setIncludeInBudget(sem.includeInBudget());
    }

    private List<String> buildQualityFlags(CategoryAssetDto asset) {
        List<String> flags = new ArrayList<>();
        if (asset.getTransactionCount() == 0) {
            flags.add("empty");
        }
        if (asset.getTransactionCount() > 0 && asset.getActiveRuleCount() == 0) {
            flags.add("no_active_rules");
        }
        if (asset.getOrphanRuleCount() > 0) {
            flags.add("orphan_rules");
        }
        String code = StringUtils.defaultString(asset.getCategoryCode());
        if (code.startsWith("OTHER") && asset.getTransactionCount() >= 20) {
            flags.add("other_expense_concentration");
        }
        return flags;
    }

    private long aggregateTxnCountForCategory(ConsumeCategory cat, Map<String, Long> txnByCode) {
        long total = 0;
        for (String ref : CategoryImpactSupport.categoryRefs(cat)) {
            total += txnByCode.getOrDefault(ref, 0L);
        }
        if (CategoryMergeSupport.isLevelOne(cat) && StringUtils.isNotBlank(cat.getCode())) {
            for (ConsumeCategory child : listActiveChildren(cat.getCode())) {
                for (String ref : CategoryImpactSupport.categoryRefs(child)) {
                    total += txnByCode.getOrDefault(ref, 0L);
                }
            }
        }
        return total;
    }

    private long aggregateActiveRulesForCategory(ConsumeCategory cat, Map<String, Long> rulesByRef) {
        long total = 0;
        for (String ref : CategoryImpactSupport.categoryRefs(cat)) {
            total += rulesByRef.getOrDefault(ref, 0L);
        }
        if (CategoryMergeSupport.isLevelOne(cat) && StringUtils.isNotBlank(cat.getCode())) {
            for (ConsumeCategory child : listActiveChildren(cat.getCode())) {
                for (String ref : CategoryImpactSupport.categoryRefs(child)) {
                    total += rulesByRef.getOrDefault(ref, 0L);
                }
            }
        }
        return total;
    }

    private Map<String, Long> loadTransactionCountsByCode() {
        String sql = "select t.consume_code as code, count(*) as cnt from `transaction` t "
                + "where coalesce(t.deleted,0)=0 and t.consume_code is not null and trim(t.consume_code) <> '' "
                + "group by t.consume_code";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        Map<String, Long> out = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object code = row.get("code");
            Object cnt = row.get("cnt");
            if (code != null && cnt instanceof Number n) {
                out.put(String.valueOf(code).trim(), n.longValue());
            }
        }
        return out;
    }

    private Map<String, Long> loadActiveRuleCountsByCategoryRef() {
        List<ConsumeRule> rules = ruleService.list();
        Map<String, Long> out = new HashMap<>();
        for (ConsumeRule rule : rules) {
            if (!OrphanRuleSupport.isActive(rule)) {
                continue;
            }
            String catId = StringUtils.trimToNull(rule.getCategoryId());
            if (catId == null) {
                continue;
            }
            out.merge(catId, 1L, Long::sum);
        }
        return out;
    }

    private List<ConsumeCategory> listActiveCategories() {
        LambdaQueryWrapper<ConsumeCategory> qw = Wrappers.lambdaQuery();
        qw.ne(ConsumeCategory::getDeleted, 1);
        return categoryService.list(qw);
    }

    private List<ConsumeCategory> listActiveChildren(String parentCode) {
        LambdaQueryWrapper<ConsumeCategory> qw = Wrappers.lambdaQuery();
        qw.eq(ConsumeCategory::getParentId, parentCode).ne(ConsumeCategory::getDeleted, 1);
        return categoryService.list(qw);
    }

    private Set<String> loadAllCategoryCodes() {
        return categoryService.list().stream()
                .map(ConsumeCategory::getCode)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private long countTransactions(List<String> refs) {
        if (refs.isEmpty()) {
            return 0;
        }
        Long count = jdbcTemplate.queryForObject(
                CategoryStatsSupport.countTransactionsSql(refs),
                Long.class,
                CategoryImpactSupport.transactionMatchParams(refs));
        return count == null ? 0 : count;
    }

    private double sumTransactionAmount(List<String> refs) {
        if (refs.isEmpty()) {
            return 0;
        }
        Double total = jdbcTemplate.queryForObject(
                CategoryStatsSupport.sumTransactionAmountSql(refs),
                Double.class,
                CategoryImpactSupport.transactionMatchParams(refs));
        return total == null ? 0 : total;
    }

    private Date loadLastTransactionDate(List<String> refs) {
        if (refs.isEmpty()) {
            return null;
        }
        return jdbcTemplate.queryForObject(
                CategoryStatsSupport.lastTransactionDateSql(refs),
                Date.class,
                CategoryImpactSupport.transactionMatchParams(refs));
    }

    private static String formatLastDate(Date date) {
        if (date == null) {
            return null;
        }
        LocalDate local = date.toLocalDate();
        return local.toString();
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

    private long countOrphanRulesForCategory(ConsumeCategory cat,
                                             Set<String> activeIds,
                                             Set<String> activeCodes) {
        boolean categoryActive = cat != null
                && (cat.getDeleted() == null || cat.getDeleted() != 1);
        if (categoryActive) {
            return 0;
        }
        long count = 0;
        for (ConsumeRule rule : ruleService.list()) {
            if (!matchesCategoryRule(rule, cat)) {
                continue;
            }
            if (OrphanRuleSupport.isActiveOrphan(rule, activeIds, activeCodes)) {
                count++;
            }
        }
        return count;
    }

    private static boolean matchesCategoryRule(ConsumeRule rule, ConsumeCategory cat) {
        if (rule == null || cat == null) {
            return false;
        }
        String catId = StringUtils.trimToEmpty(rule.getCategoryId());
        if (catId.isEmpty()) {
            return false;
        }
        return catId.equals(StringUtils.trimToEmpty(cat.getCode()))
                || catId.equals(StringUtils.trimToEmpty(cat.getId()));
    }
}

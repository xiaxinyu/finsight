package com.finsight.application.classification;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads existing category codes and builds Sprint 2 L2 seed plan (read-only).
 */
@Service
public class L2CategorySeedService {

    private final JdbcTemplate jdbcTemplate;

    public L2CategorySeedService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Set<String> loadExistingCodes() {
        if (!tableExists("cls_category")) {
            return Set.of();
        }
        List<String> codes = jdbcTemplate.queryForList(
                "select distinct code from cls_category where code is not null and trim(code) <> ''",
                String.class);
        return toCodeSet(codes);
    }

    public Set<String> loadActiveCodes() {
        if (!tableExists("cls_category")) {
            return Set.of();
        }
        List<String> codes = jdbcTemplate.queryForList(
                "select distinct code from cls_category where code is not null and trim(code) <> '' "
                        + "and coalesce(deleted, 0) = 0",
                String.class);
        return toCodeSet(codes);
    }

    private static Set<String> toCodeSet(List<String> codes) {
        Set<String> out = new HashSet<>();
        for (String code : codes) {
            if (code != null && !code.isBlank()) {
                out.add(code.trim());
            }
        }
        return out;
    }

    public Map<String, Object> buildSeedPlan() {
        L2CategorySeedPlanner.validateCatalog();
        Set<String> existing = loadExistingCodes();
        List<L2CategorySeedPlanner.SeedItem> items = L2CategorySeedPlanner.buildInsertPlan(existing);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("existingCodeCount", existing.size());
        out.put("insertCount", items.stream().filter(i -> i.action() == L2CategorySeedPlanner.Action.INSERT).count());
        out.put("skipExistsCount", items.stream().filter(i -> i.action() == L2CategorySeedPlanner.Action.SKIP_EXISTS).count());
        out.put("catalogOnlyCount", items.stream().filter(i -> i.action() == L2CategorySeedPlanner.Action.SKIP_CATALOG_ONLY).count());
        out.put("items", items);
        out.put("nameUpdates", L2CategorySeedPlanner.buildNameUpdates());
        out.put("duplicateL1Hints", buildDuplicateL1Hints(loadActiveCodes()));
        out.put("manualScript", "docs/tech/database/l2-category-sprint2-seed.sql");
        out.put("dedupPlaybook", "docs/tech/database/category-dedup-merge-playbook.zh-cn.md");
        out.put("catalogDoc", "docs/tech/database/classification-l2-target-catalog.zh-cn.md");
        out.put("note", "Does not mutate data — run manual SQL after review. Prefer UI merge per dedup playbook.");
        return out;
    }

    private List<Map<String, String>> buildDuplicateL1Hints(Set<String> active) {
        List<Map<String, String>> hints = new ArrayList<>();
        if (active.contains("INC") && active.contains("INCOME")) {
            hints.add(Map.of(
                    "pair", "INC + INCOME",
                    "action", "Merge INCOME (source) into INC (target) via Categories UI",
                    "doc", "docs/tech/database/category-dedup-merge-playbook.zh-cn.md"));
        }
        if (active.contains("TRANSPORT") && active.contains("TRAVEL")) {
            hints.add(Map.of(
                    "pair", "TRANSPORT + TRAVEL",
                    "action", "Merge TRAVEL (source) into TRANSPORT (target) via Categories UI",
                    "doc", "docs/tech/database/category-dedup-merge-playbook.zh-cn.md"));
        }
        return hints;
    }

    private boolean tableExists(String table) {
        Integer n = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_schema = database() and table_name = ?",
                Integer.class,
                table);
        return n != null && n > 0;
    }
}

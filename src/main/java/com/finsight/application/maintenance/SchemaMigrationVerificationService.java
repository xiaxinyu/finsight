package com.finsight.application.maintenance;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SchemaMigrationVerificationService {

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationVerificationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> verify() {
        Map<String, Object> out = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();

        for (String table : List.of(
                "transaction", "imp_staging_entry", "statement", "fin_bank_account",
                "cls_category", "cls_rule", "cls_rule_tag", "fs_user", "ben_contribution")) {
            if (!tableExists(table)) {
                missing.add(table);
            }
        }
        out.put("missingCoreTables", missing);

        out.put("transactionRows", count("transaction"));
        out.put("stagingRows", count("imp_staging_entry"));
        out.put("bankAccountRows", count("fin_bank_account"));
        out.put("benefitRows", count("ben_contribution"));
        out.put("fsUserRows", count("fs_user"));
        out.put("clsCategoryRows", count("cls_category"));
        out.put("clsRuleRows", count("cls_rule"));
        out.put("clsRuleActiveRows", countActiveRules());
        out.put("orphanRuleRows", countActiveOrphanRules());
        out.put("archivedLegacyOrphanRuleRows", countArchivedLegacyOrphanRules());
        out.put("blankPatternRuleRows", countBlankPatternRules());
        out.put("activeBlankPatternRuleRows", countActiveBlankPatternRules());
        out.put("archivedInvalidPatternRuleRows", countArchivedInvalidPatternRules());
        out.put("archivedConsumeCategory", tableExists("_archive_consume_category"));
        out.put("archivedConsumeRule", tableExists("_archive_consume_rule"));
        out.put("leftoverLegacyTables", listLeftoverLegacyTables());

        Double income = jdbcTemplate.queryForObject(
                "select coalesce(sum(income_money),0) from transaction where coalesce(deleted,0)=0",
                Double.class);
        Double expense = queryExpenseTotal();
        out.put("incomeTotal", income);
        out.put("expenseTotal", expense);

        boolean ok = missing.isEmpty();
        out.put("ok", ok);
        return out;
    }

    private long countActiveRules() {
        if (!tableExists("cls_rule")) {
            return -1;
        }
        Long c = jdbcTemplate.queryForObject(
                "select count(*) from cls_rule where coalesce(active,1)=1 and pattern is not null and trim(pattern)<>''",
                Long.class);
        return c == null ? 0 : c;
    }

    private long countActiveOrphanRules() {
        if (!tableExists("cls_rule") || !tableExists("cls_category")) {
            return -1;
        }
        Long c = jdbcTemplate.queryForObject(
                "select count(*) from cls_rule r "
                        + "left join cls_category c on (c.code = r.category_id or c.id = r.category_id) "
                        + "and coalesce(c.deleted,0)=0 "
                        + "where r.category_id is not null and trim(r.category_id)<>'' "
                        + "and c.id is null "
                        + "and coalesce(r.active,1)=1 "
                        + "and coalesce(r.remark,'') not like '%[inactive legacy:%' "
                        + "and coalesce(r.remark,'') not like '%[auto-disabled: orphan%'",
                Long.class);
        return c == null ? 0 : c;
    }

    private long countArchivedLegacyOrphanRules() {
        if (!tableExists("cls_rule") || !tableExists("cls_category")) {
            return -1;
        }
        Long c = jdbcTemplate.queryForObject(
                "select count(*) from cls_rule r "
                        + "left join cls_category c on (c.code = r.category_id or c.id = r.category_id) "
                        + "and coalesce(c.deleted,0)=0 "
                        + "where r.category_id is not null and trim(r.category_id)<>'' "
                        + "and c.id is null "
                        + "and coalesce(r.active,1)=0 "
                        + "and (coalesce(r.remark,'') like '%[inactive legacy:%' "
                        + "     or coalesce(r.remark,'') like '%[auto-disabled: orphan%')",
                Long.class);
        return c == null ? 0 : c;
    }

    private long countBlankPatternRules() {
        if (!tableExists("cls_rule")) {
            return -1;
        }
        Long c = jdbcTemplate.queryForObject(
                "select count(*) from cls_rule where pattern is null or trim(pattern)=''",
                Long.class);
        return c == null ? 0 : c;
    }

    private long countActiveBlankPatternRules() {
        if (!tableExists("cls_rule")) {
            return -1;
        }
        Long c = jdbcTemplate.queryForObject(
                "select count(*) from cls_rule "
                        + "where (pattern is null or trim(pattern)='') "
                        + "and coalesce(active,1)=1",
                Long.class);
        return c == null ? 0 : c;
    }

    private long countArchivedInvalidPatternRules() {
        if (!tableExists("cls_rule")) {
            return -1;
        }
        Long c = jdbcTemplate.queryForObject(
                "select count(*) from cls_rule "
                        + "where (pattern is null or trim(pattern)='') "
                        + "and coalesce(active,1)=0 "
                        + "and (coalesce(remark,'') like '%[auto-disabled: blank pattern]%' "
                        + "     or coalesce(remark,'') like '%[inactive legacy: blank pattern]%')",
                Long.class);
        return c == null ? 0 : c;
    }

    private Double queryExpenseTotal() {
        if (columnExists("transaction", "expense_amount")) {
            return jdbcTemplate.queryForObject(
                    "select coalesce(sum(expense_amount),0) from transaction "
                            + "where coalesce(deleted,0)=0 and coalesce(expense_amount,0)>0",
                    Double.class);
        }
        return jdbcTemplate.queryForObject(
                "select coalesce(sum(balance_money),0) from transaction "
                        + "where coalesce(deleted,0)=0 and coalesce(balance_money,0)>0",
                Double.class);
    }

    private boolean columnExists(String table, String column) {
        Integer n = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_schema = database() and table_name = ? and column_name = ?",
                Integer.class,
                table,
                column);
        return n != null && n > 0;
    }

    private long count(String table) {
        if (!tableExists(table)) {
            return -1;
        }
        Long c = jdbcTemplate.queryForObject("select count(*) from `" + table + "`", Long.class);
        return c == null ? 0 : c;
    }

    private boolean tableExists(String table) {
        Integer n = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = database() and table_name = ?",
                Integer.class,
                table);
        return n != null && n > 0;
    }

    private List<String> listLeftoverLegacyTables() {
        List<String> leftovers = new ArrayList<>();
        for (String table : List.of(
                "_deprecated_medical", "_deprecated_endowment", "_deprecated_accumulation",
                "_deprecated_unemployment", "_deprecated_bank_card",
                "auth_user", "django_migrations", "deposit", "CREDIT", "card",
                "medical", "endowment", "accumulation", "unemployment")) {
            if (tableExists(table)) {
                leftovers.add(table);
            }
        }
        return leftovers;
    }
}

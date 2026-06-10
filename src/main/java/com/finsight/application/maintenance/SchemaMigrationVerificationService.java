package com.finsight.application.maintenance;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SchemaMigrationVerificationService {

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationVerificationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> verify() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("transactionRows", count("transaction"));
        out.put("stagingRows", count("imp_staging_entry"));
        out.put("bankAccountRows", count("fin_bank_account"));
        out.put("benefitRows", count("ben_contribution"));
        out.put("fsUserRows", count("fs_user"));
        out.put("clsCategoryRows", count("cls_category"));

        Double income = jdbcTemplate.queryForObject(
                "select coalesce(sum(income_money),0) from transaction where coalesce(deleted,0)=0",
                Double.class);
        Double expense = jdbcTemplate.queryForObject(
                "select coalesce(sum(balance_money),0) from transaction where coalesce(deleted,0)=0 and coalesce(balance_money,0)>0",
                Double.class);
        out.put("incomeTotal", income);
        out.put("expenseTotal", expense);
        out.put("ok", true);
        return out;
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
}

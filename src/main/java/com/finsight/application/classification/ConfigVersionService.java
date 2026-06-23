package com.finsight.application.classification;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ConfigVersionService {

    private final JdbcTemplate jdbcTemplate;

    public ConfigVersionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> current() {
        ensureRow();
        return jdbcTemplate.queryForMap(
                "select taxonomy_version, rule_set_version, metric_refresh_version, updatetime "
                        + "from fin_config_version where id = 1");
    }

    public void bumpTaxonomy() {
        ensureRow();
        jdbcTemplate.update("update fin_config_version set taxonomy_version = taxonomy_version + 1 where id = 1");
    }

    public void bumpRuleSet() {
        ensureRow();
        jdbcTemplate.update("update fin_config_version set rule_set_version = rule_set_version + 1 where id = 1");
    }

    public void bumpMetricRefresh() {
        ensureRow();
        jdbcTemplate.update(
                "update fin_config_version set metric_refresh_version = metric_refresh_version + 1 where id = 1");
    }

    public Map<String, Object> asMap() {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            Map<String, Object> row = current();
            out.put("taxonomyVersion", row.get("taxonomy_version"));
            out.put("ruleSetVersion", row.get("rule_set_version"));
            out.put("metricRefreshVersion", row.get("metric_refresh_version"));
            out.put("updatedAt", row.get("updatetime"));
        } catch (Exception ignored) {
            out.put("taxonomyVersion", 1);
            out.put("ruleSetVersion", 1);
            out.put("metricRefreshVersion", 1);
        }
        return out;
    }

    private void ensureRow() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from fin_config_version where id = 1", Integer.class);
        if (count == null || count == 0) {
            jdbcTemplate.update(
                    "insert into fin_config_version (id, taxonomy_version, rule_set_version, metric_refresh_version) "
                            + "values (1, 1, 1, 1)");
        }
    }
}

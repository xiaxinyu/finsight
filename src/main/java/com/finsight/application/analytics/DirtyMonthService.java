package com.finsight.application.analytics;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DirtyMonthService {

    private final JdbcTemplate jdbcTemplate;
    private final MetricMonthlyService metricMonthlyService;
    private final ConfigVersionBump configVersionBump;

    public DirtyMonthService(JdbcTemplate jdbcTemplate,
                             MetricMonthlyService metricMonthlyService,
                             ConfigVersionBump configVersionBump) {
        this.jdbcTemplate = jdbcTemplate;
        this.metricMonthlyService = metricMonthlyService;
        this.configVersionBump = configVersionBump;
    }

    public void markDirty(Collection<String> monthKeys) {
        if (monthKeys == null) {
            return;
        }
        for (String month : monthKeys) {
            if (month == null || month.isBlank()) {
                continue;
            }
            jdbcTemplate.update(
                    "insert into fin_dirty_month (month_key) values (?) "
                            + "on duplicate key update marked_at = current_timestamp, refreshed_at = null",
                    month.trim());
        }
    }

    public List<Map<String, Object>> listDirty() {
        return jdbcTemplate.query(
                "select month_key as monthKey, marked_at as markedAt, refreshed_at as refreshedAt "
                        + "from fin_dirty_month where refreshed_at is null order by month_key",
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("monthKey", rs.getString("monthKey"));
                    row.put("markedAt", rs.getTimestamp("markedAt"));
                    row.put("refreshedAt", rs.getTimestamp("refreshedAt"));
                    return row;
                });
    }

    public Map<String, Object> refreshAllDirty() throws Exception {
        List<Map<String, Object>> dirty = listDirty();
        List<String> refreshed = new ArrayList<>();
        for (Map<String, Object> row : dirty) {
            String month = String.valueOf(row.get("monthKey"));
            metricMonthlyService.refresh(month);
            jdbcTemplate.update(
                    "update fin_dirty_month set refreshed_at = current_timestamp where month_key = ?",
                    month);
            refreshed.add(month);
        }
        configVersionBump.bumpMetricRefresh();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("refreshedMonths", refreshed);
        out.put("count", refreshed.size());
        return out;
    }
}

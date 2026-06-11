package com.finsight.application.analytics;

import com.finsight.application.support.ListingDateSupport;
import com.finsight.common.exception.AppServiceException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsExportService {

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsExportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> exportRows(String startStr, String endStr, int limit) throws AppServiceException {
        int cap = limit <= 0 ? 5000 : Math.min(limit, 20000);
        Date[] range = ListingDateSupport.parseMmDdYyyyOrDefaultOneYear(
                StringUtils.defaultIfBlank(startStr, ""),
                StringUtils.defaultIfBlank(endStr, ""));
        String sql = """
                SELECT txn_date, direction, amount, category_code, category_name,
                       category_l1_code, category_l1_name, bank_code, card_type_code,
                       transaction_desc, opponent_name, memo, is_transfer, statement_id
                FROM v_transaction_analytics
                WHERE txn_date >= ? AND txn_date <= ?
                ORDER BY txn_date DESC, amount DESC
                LIMIT ?
                """;
        return jdbcTemplate.queryForList(sql, range[0], range[1], cap);
    }

    public String toCsv(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "txn_date,direction,amount,category_code,category_name,category_l1_code,category_l1_name,bank_code,card_type_code,transaction_desc,opponent_name,memo,is_transfer,statement_id\n";
        }
        String[] cols = rows.get(0).keySet().toArray(new String[0]);
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", cols)).append('\n');
        for (Map<String, Object> row : rows) {
            for (int i = 0; i < cols.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(csvEscape(row.get(cols[i])));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String csvEscape(Object value) {
        if (value == null) {
            return "";
        }
        String s = String.valueOf(value);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}

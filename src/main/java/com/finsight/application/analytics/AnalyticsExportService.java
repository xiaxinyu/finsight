package com.finsight.application.analytics;

import com.finsight.application.authentication.LedgerUserScope;
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

    private static final String OWNER_FILTER = """
            AND (t.created_by = ? OR (? = '_anonymous' AND (t.created_by IS NULL OR TRIM(t.created_by) = '')))
            """;

    private static final String SELECT_COLUMNS = """
            v.txn_date, v.direction, v.amount, v.category_code, v.category_name,
            v.category_l1_code, v.category_l1_name, v.bank_code, v.card_type_code,
            v.transaction_desc, v.opponent_name, v.memo, v.is_transfer, v.statement_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final LedgerUserScope ledgerUserScope;

    public AnalyticsExportService(JdbcTemplate jdbcTemplate, LedgerUserScope ledgerUserScope) {
        this.jdbcTemplate = jdbcTemplate;
        this.ledgerUserScope = ledgerUserScope;
    }

    public List<Map<String, Object>> exportRows(String startStr, String endStr, int limit) throws AppServiceException {
        int cap = limit <= 0 ? 5000 : Math.min(limit, 20000);
        String owner = ledgerUserScope.resolve();
        Date[] range = ListingDateSupport.parseMmDdYyyyOrNull(
                StringUtils.defaultIfBlank(startStr, ""),
                StringUtils.defaultIfBlank(endStr, ""));
        if (range[0] != null && range[1] != null) {
            String sql = """
                    SELECT %s
                    FROM v_transaction_analytics v
                    INNER JOIN transaction t ON t.id = v.id
                    WHERE (t.deleted IS NULL OR t.deleted = 0)
                    %s
                    AND v.txn_date >= ? AND v.txn_date <= ?
                    ORDER BY v.txn_date DESC, v.amount DESC
                    LIMIT ?
                    """.formatted(SELECT_COLUMNS, OWNER_FILTER);
            return jdbcTemplate.queryForList(sql, owner, owner, range[0], range[1], cap);
        }
        String sql = """
                SELECT %s
                FROM v_transaction_analytics v
                INNER JOIN transaction t ON t.id = v.id
                WHERE (t.deleted IS NULL OR t.deleted = 0)
                %s
                ORDER BY v.txn_date DESC, v.amount DESC
                LIMIT ?
                """.formatted(SELECT_COLUMNS, OWNER_FILTER);
        return jdbcTemplate.queryForList(sql, owner, owner, cap);
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

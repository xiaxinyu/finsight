package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MerchantMiningService {

    private final JdbcTemplate jdbcTemplate;
    private final AuthenticationFacade authenticationFacade;

    public MerchantMiningService(JdbcTemplate jdbcTemplate, AuthenticationFacade authenticationFacade) {
        this.jdbcTemplate = jdbcTemplate;
        this.authenticationFacade = authenticationFacade;
    }

    public Map<String, Object> refreshProfiles() {
        String userId = userKey();
        List<Map<String, Object>> merchants = jdbcTemplate.queryForList(
                "select merchant_token, count(*) as txn_count, round(avg(amount),2) as avg_amount, "
                        + "max(txn_date) as last_seen "
                        + "from v_transaction_analytics "
                        + "where merchant_token is not null and merchant_token != '' "
                        + "group by merchant_token having count(*) >= 3 "
                        + "order by txn_count desc limit 100");

        int upserted = 0;
        for (Map<String, Object> row : merchants) {
            String token = String.valueOf(row.get("merchant_token"));
            int count = ((Number) row.get("txn_count")).intValue();
            boolean subscription = count >= 6;
            jdbcTemplate.update(
                    "insert into fin_merchant_profile (id, user_id, merchant_token, display_name, is_subscription, "
                            + "avg_amount, txn_count, last_seen, updated_at) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, now(3)) "
                            + "on duplicate key update avg_amount=values(avg_amount), txn_count=values(txn_count), "
                            + "is_subscription=values(is_subscription), last_seen=values(last_seen), updated_at=now(3)",
                    UUID.randomUUID().toString(), userId, token, token, subscription ? 1 : 0,
                    row.get("avg_amount"), count, row.get("last_seen"));
            upserted++;
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("upserted", upserted);
        out.put("subscriptions", merchants.stream().filter(m -> ((Number) m.get("txn_count")).intValue() >= 6).count());
        return out;
    }

    public List<Map<String, Object>> subscriptions() {
        return jdbcTemplate.queryForList(
                "select merchant_token as merchantToken, display_name as displayName, avg_amount as avgAmount, txn_count as txnCount "
                        + "from fin_merchant_profile where user_id = ? and is_subscription = 1 order by avg_amount desc",
                userKey());
    }

    private String userKey() {
        String user = authenticationFacade.getUserName();
        return user == null || user.isBlank() ? "_anonymous" : user;
    }
}

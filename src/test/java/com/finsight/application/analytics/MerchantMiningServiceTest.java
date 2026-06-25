package com.finsight.application.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsight.application.authentication.AuthenticationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantMiningServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private AuthenticationFacade authenticationFacade;

    private MerchantMiningService service;

    @BeforeEach
    void setUp() {
        service = new MerchantMiningService(jdbcTemplate, authenticationFacade, new ObjectMapper());
        when(authenticationFacade.getUserName()).thenReturn("user1");
    }

    @Test
    void refreshProfiles_marksMonthlySubscription() {
        when(jdbcTemplate.queryForList(contains("v_transaction_finance_semantics"), eq("user1"), eq("user1")))
                .thenReturn(netflixRows());

        Map<String, Object> out = service.refreshProfiles();

        assertEquals(1, out.get("upserted"));
        assertEquals(1, out.get("subscriptions"));

        ArgumentCaptor<Integer> subscriptionFlag = ArgumentCaptor.forClass(Integer.class);
        verify(jdbcTemplate, atLeastOnce()).update(
                contains("fin_merchant_profile"),
                any(), eq("user1"), eq("netflix"), anyString(),
                subscriptionFlag.capture(),
                any(), any(), any(Date.class), anyString());
        assertEquals(1, subscriptionFlag.getValue());
    }

    @Test
    void subscriptionReport_includesSummaryTotals() {
        stubSubscriptionPeriodQueries(netflixRows());

        @SuppressWarnings("unchecked")
        Map<String, Object> out = service.subscriptionReport();
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) out.get("summary");

        assertNotNull(summary.get("monthlyTotal"));
        assertNotNull(summary.get("optimizableAmount"));
        assertNotNull(summary.get("periodStart"));
        assertTrue(((Number) summary.get("count")).intValue() >= 1);
    }

    @Test
    void concentration_returnsTopShare() {
        when(jdbcTemplate.queryForList(contains("fin_merchant_profile"), eq("user1")))
                .thenReturn(List.of(
                        profileRow(),
                        Map.of(
                                "merchant_token", "amazon",
                                "display_name", "Amazon",
                                "avg_amount", 50,
                                "txn_count", 4,
                                "is_subscription", 0,
                                "payload_json", "{}")));

        Map<String, Object> out = service.concentration();

        assertTrue(((Number) out.get("totalSpend")).doubleValue() > 0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> merchants = (List<Map<String, Object>>) out.get("merchants");
        assertEquals(2, merchants.size());
        assertNotNull(out.get("top3SharePct"));
        assertNotNull(out.get("top1SharePct"));
        assertNotNull(out.get("top5SharePct"));
        assertNotNull(merchants.get(0).get("drillDown"));
    }

    @Test
    void subscriptionReport_exposesEvidenceAndDrillDown() {
        stubSubscriptionPeriodQueries(netflixRows());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subs = (List<Map<String, Object>>) service.subscriptionReport().get("subscriptions");
        assertEquals(1, subs.size());
        assertNotNull(subs.get(0).get("evidence"));
        assertNotNull(subs.get(0).get("avgIntervalDays"));
        assertNotNull(subs.get(0).get("drillDown"));
    }

    @Test
    void drift_splitsMerchantBuckets() {
        when(jdbcTemplate.queryForList(contains("v.txn_date >="),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2027, 1, 1)), eq("user1"), eq("user1")))
                .thenReturn(List.of(
                        txnRow("2026-01-05", 100, "NewShop"),
                        txnRow("2026-02-04", 100, "NewShop"),
                        txnRow("2026-03-05", 150, "GrowingCo"),
                        txnRow("2026-04-04", 150, "GrowingCo")));
        when(jdbcTemplate.queryForList(contains("v.txn_date >="),
                eq(LocalDate.of(2025, 1, 1)), eq(LocalDate.of(2026, 1, 1)), eq("user1"), eq("user1")))
                .thenReturn(List.of(
                        txnRow("2025-01-05", 50, "GrowingCo"),
                        txnRow("2025-06-04", 50, "GrowingCo"),
                        txnRow("2025-01-05", 200, "DecliningCo"),
                        txnRow("2025-06-04", 200, "DecliningCo")));

        Map<String, Object> out = service.drift(2026);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> newMerchants = (List<Map<String, Object>>) out.get("newMerchants");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> growing = (List<Map<String, Object>>) out.get("growingMerchants");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> declining = (List<Map<String, Object>>) out.get("decliningMerchants");
        assertFalse(newMerchants.isEmpty());
        assertFalse(growing.isEmpty());
        assertFalse(declining.isEmpty());
        assertNotNull(newMerchants.get(0).get("drillDown"));
    }

    private void stubSubscriptionPeriodQueries(List<Map<String, Object>> patternRows) {
        int year = LocalDate.now().getYear();
        Date periodStart = Date.valueOf(LocalDate.of(year, 1, 1));
        Date periodEnd = Date.valueOf(LocalDate.of(year + 1, 1, 1));

        when(jdbcTemplate.queryForList(
                argThat((String sql) -> sql != null && sql.contains("v.txn_date >=") && sql.contains("order by v.txn_date desc")),
                eq(periodStart),
                eq(periodEnd),
                eq("user1"),
                eq("user1")))
                .thenReturn(rawList(patternRows));
        when(jdbcTemplate.queryForList(
                argThat((String sql) -> sql != null && sql.contains("count(distinct v.id)")),
                eq(periodStart),
                eq(periodEnd),
                eq("user1"),
                eq("user1")))
                .thenReturn(rawList(List.of(Map.of("txn_count", 0, "total_spend", 0, "merchant_count", 0))));
        when(jdbcTemplate.queryForList(
                argThat((String sql) -> sql != null && sql.contains("v.merchant_token, v.opponent_name")),
                eq(periodStart),
                eq(periodEnd),
                eq("user1"),
                eq("user1")))
                .thenReturn(rawList(List.of()));
    }

    private static List<Map<String, Object>> netflixRows() {
        return List.of(
                txnRow("2026-01-05", 15.99, "Netflix.com"),
                txnRow("2026-02-04", 15.99, "Netflix Monthly"),
                txnRow("2026-03-05", 16.00, "Netflix.com 883920"),
                txnRow("2026-04-04", 15.99, "NETFLIX.COM"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List rawList(List<Map<String, Object>> rows) {
        return rows;
    }

    private static Map<String, Object> txnRow(String date, double amount, String desc) {
        return Map.of(
                "opponent_name", "",
                "transaction_desc", desc,
                "amount", amount,
                "txn_date", Date.valueOf(LocalDate.parse(date)));
    }

    private static Map<String, Object> profileRow() {
        return profileRowWithEvidence();
    }

    private static Map<String, Object> profileRowWithEvidence() {
        return Map.of(
                "merchant_token", "netflix",
                "display_name", "Netflix",
                "avg_amount", 15.99,
                "txn_count", 4,
                "last_seen", Date.valueOf(LocalDate.of(2026, 4, 4)),
                "is_subscription", 1,
                "payload_json", "{\"cadence\":\"monthly\",\"confidence\":0.92,\"avgIntervalDays\":30,\"amountCv\":0.02,\"evidence\":\"4 charges every ~30 days\"}");
    }
}

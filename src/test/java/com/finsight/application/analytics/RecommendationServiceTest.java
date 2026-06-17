package com.finsight.application.analytics;

import com.finsight.application.config.FinsightFeatureProperties;
import com.finsight.application.finance.InsightService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private FinsightFeatureProperties features;

    @Mock
    private InsightService insightService;

    @Mock
    private FinancialProfileService profileService;

    @Mock
    private ForecastService forecastService;

    @Mock
    private TrendAnalysisService trendAnalysisService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private CombinedInsightService combinedInsightService;

    @InjectMocks
    private RecommendationService service;

    @Test
    void topRecommendations_skipsDismissedProfileDimension() throws Exception {
        FinsightFeatureProperties.Advisor advisor = new FinsightFeatureProperties.Advisor();
        advisor.setEnabled(true);
        when(features.getAdvisor()).thenReturn(advisor);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("fin_recommendation_feedback"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("fin_insight_card"))).thenReturn(1);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyString()))
                .thenReturn(List.of("data_trust"));
        when(combinedInsightService.topCombinedCards(5)).thenReturn(List.of(
                Map.of(
                        "id", "combined_forecast_pressure",
                        "type", "combined",
                        "priority", 78,
                        "urgency", "high",
                        "confidence", 0.8,
                        "impactAmount", 1200),
                Map.of("id", "combined_archetype_trend", "type", "combined", "priority", 72),
                Map.of("id", "combined_subscription_review", "type", "combined", "priority", 68)
        ));

        List<Map<String, Object>> cards = service.topRecommendations("user1");

        assertTrue(cards.stream().noneMatch(c -> "data_trust".equals(c.get("id"))));
        assertEquals("combined_forecast_pressure", cards.get(0).get("id"));
        Map<String, Object> card = cards.get(0);
        assertNotNull(card.get("urgency"));
        assertNotNull(card.get("confidence"));
        assertNotNull(card.get("expiresAt"));
        assertTrue(((Number) card.get("impactAmount")).doubleValue() > 0);
        verify(combinedInsightService).topCombinedCards(5);
        verify(profileService, never()).currentProfile();
        verify(jdbcTemplate, atLeastOnce()).update(
                argThat((String sql) -> sql.contains("on duplicate key update")),
                eq("user1:combined_forecast_pressure"),
                eq("user1"),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void storageId_scopesCardKeyPerUser() {
        assertEquals("alice:income_stability", RecommendationService.storageId("alice", "income_stability"));
        assertEquals("bob:income_stability", RecommendationService.storageId("bob", "income_stability"));
    }

    @Test
    void feedback_snoozeSetsShortSnoozeWindow() {
        service.feedback("user1", "liquidity_safety", "snooze");
        verify(jdbcTemplate).update(
                argThat((String sql) -> sql.contains("fin_recommendation_feedback")),
                any(), eq("user1"), eq("liquidity_safety"), eq("snooze"), any());
    }

    @Test
    void feedback_acceptRecordsWithoutSnoozeExpiry() {
        service.feedback("user1", "cashflow_risk", "accept");
        verify(jdbcTemplate).update(
                argThat((String sql) -> sql.contains("fin_recommendation_feedback")),
                any(), eq("user1"), eq("cashflow_risk"), eq("accept"), eq(null));
    }

    @Test
    void topRecommendations_legacyWhenAdvisorDisabled() throws Exception {
        FinsightFeatureProperties.Advisor advisor = new FinsightFeatureProperties.Advisor();
        advisor.setEnabled(false);
        when(features.getAdvisor()).thenReturn(advisor);
        when(insightService.decisionCards()).thenReturn(List.of(Map.of("title", "Stable", "type", "info")));

        List<Map<String, Object>> cards = service.topRecommendations("user1");

        assertTrue(!cards.isEmpty());
        verify(profileService, never()).currentProfile();
    }
}

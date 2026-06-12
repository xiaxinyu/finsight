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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @InjectMocks
    private RecommendationService service;

    @Test
    void topRecommendations_skipsDismissedProfileDimension() throws Exception {
        FinsightFeatureProperties.Advisor advisor = new FinsightFeatureProperties.Advisor();
        advisor.setEnabled(true);
        when(features.getAdvisor()).thenReturn(advisor);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("fin_recommendation_feedback"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("fin_insight_card"))).thenReturn(0);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyString()))
                .thenReturn(List.of("data_trust"));
        when(profileService.currentProfile()).thenReturn(Map.of(
                "dimensions", List.of(
                        Map.of("id", "data_trust", "score", 40, "summary", "classify more"),
                        Map.of("id", "liquidity_safety", "score", 50, "summary", "low runway")
                )
        ));
        when(forecastService.forecast(any(Integer.class), anyString())).thenReturn(Map.of("deficitMonths", List.of()));

        List<Map<String, Object>> cards = service.topRecommendations("user1");

        assertTrue(cards.stream().noneMatch(c -> "data_trust".equals(c.get("id"))));
        verify(profileService).currentProfile();
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

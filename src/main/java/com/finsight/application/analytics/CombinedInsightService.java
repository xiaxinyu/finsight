package com.finsight.application.analytics;

import com.finsight.application.config.FinsightFeatureProperties;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CombinedInsightService {

    private static final long ADVISOR_PROFILE_BUDGET_MS = 800;
    private static final long ADVISOR_TREND_BUDGET_MS = 600;
    private static final long ADVISOR_FORECAST_BUDGET_MS = 1000;
    private static final long ADVISOR_MERCHANT_BUDGET_MS = 600;

    private final FinsightFeatureProperties features;
    private final FinancialProfileService profileService;
    private final TrendAnalysisService trendAnalysisService;
    private final ForecastService forecastService;
    private final MerchantMiningService merchantMiningService;

    public CombinedInsightService(FinsightFeatureProperties features,
                                  FinancialProfileService profileService,
                                  TrendAnalysisService trendAnalysisService,
                                  ForecastService forecastService,
                                  MerchantMiningService merchantMiningService) {
        this.features = features;
        this.profileService = profileService;
        this.trendAnalysisService = trendAnalysisService;
        this.forecastService = forecastService;
        this.merchantMiningService = merchantMiningService;
    }

    public CombinedInsightContext buildContext() throws Exception {
        Map<String, Object> profile;
        if (features.getProfile().isEnabled()) {
            try (AnalyticsTiming.TimedCall ignored = AnalyticsTiming.start("advisor.profile", ADVISOR_PROFILE_BUDGET_MS)) {
                profile = profileService.currentProfile();
            }
        } else {
            profile = Map.of("userType", "balanced", "userTypeExplanation", "Profile module disabled", "dimensions", List.of());
        }

        int year = YearMonth.now().getYear();
        Map<String, Object> trends;
        if (features.getForecast().isEnabled()) {
            try (AnalyticsTiming.TimedCall ignored = AnalyticsTiming.start("advisor.trends", ADVISOR_TREND_BUDGET_MS)) {
                trends = trendAnalysisService.trends(year - 1, year);
            }
        } else {
            trends = Map.of();
        }

        Map<String, Object> forecast;
        if (features.getForecast().isEnabled()) {
            try (AnalyticsTiming.TimedCall ignored = AnalyticsTiming.start("advisor.forecast", ADVISOR_FORECAST_BUDGET_MS)) {
                forecast = forecastService.forecast(year, "base");
            }
        } else {
            forecast = Map.of();
        }

        Map<String, Object> subscriptions;
        Map<String, Object> concentration;
        if (features.getMerchantMining().isEnabled()) {
            try (AnalyticsTiming.TimedCall ignored = AnalyticsTiming.start("advisor.merchants", ADVISOR_MERCHANT_BUDGET_MS)) {
                subscriptions = merchantMiningService.subscriptionReport();
                concentration = merchantMiningService.concentration();
            }
        } else {
            subscriptions = Map.of();
            concentration = Map.of();
        }

        List<Map<String, Object>> cards = CombinedInsightBuilder.build(
                profile, trends, forecast, subscriptions, concentration);
        return new CombinedInsightContext(profile, trends, forecast, subscriptions, concentration, cards);
    }

    public List<Map<String, Object>> buildCombinedCards() throws Exception {
        return new ArrayList<>(buildContext().cards());
    }

    public List<Map<String, Object>> topCombinedCards(int limit) throws Exception {
        return buildContext().topCards(limit);
    }
}

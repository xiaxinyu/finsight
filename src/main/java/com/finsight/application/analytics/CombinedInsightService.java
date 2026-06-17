package com.finsight.application.analytics;

import com.finsight.application.config.FinsightFeatureProperties;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CombinedInsightService {

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

    public List<Map<String, Object>> buildCombinedCards() throws Exception {
        Map<String, Object> profile = features.getProfile().isEnabled()
                ? profileService.currentProfile()
                : Map.of("userType", "balanced", "userTypeExplanation", "Profile module disabled", "dimensions", List.of());

        int year = YearMonth.now().getYear();
        Map<String, Object> trends = features.getForecast().isEnabled()
                ? trendAnalysisService.trends(year - 1, year)
                : Map.of();

        Map<String, Object> forecast = features.getForecast().isEnabled()
                ? forecastService.forecast(year, "base")
                : Map.of();

        Map<String, Object> subscriptions = features.getMerchantMining().isEnabled()
                ? merchantMiningService.subscriptionReport()
                : Map.of();

        Map<String, Object> concentration = features.getMerchantMining().isEnabled()
                ? merchantMiningService.concentration()
                : Map.of();

        return CombinedInsightBuilder.build(profile, trends, forecast, subscriptions, concentration);
    }

    public List<Map<String, Object>> topCombinedCards(int limit) throws Exception {
        List<Map<String, Object>> cards = new ArrayList<>(buildCombinedCards());
        cards.sort((a, b) -> Integer.compare(
                ((Number) b.getOrDefault("priority", 0)).intValue(),
                ((Number) a.getOrDefault("priority", 0)).intValue()));
        if (cards.size() <= limit) {
            return cards;
        }
        return new ArrayList<>(cards.subList(0, limit));
    }
}

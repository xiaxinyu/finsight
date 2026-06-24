package com.finsight.application.analytics;

import com.finsight.application.config.FinsightFeatureProperties;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.LinkedHashMap;
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
        Map<String, Object> profile = loadProfile();
        int year = YearMonth.now().getYear();
        Map<String, Object> trends = loadTrends(year);
        Map<String, Object> forecast = loadForecast(year);
        Map<String, Object> subscriptions;
        Map<String, Object> concentration;
        if (features.getMerchantMining().isEnabled()) {
            try (AnalyticsTiming.TimedCall ignored = AnalyticsTiming.start("advisor.merchants", ADVISOR_MERCHANT_BUDGET_MS)) {
                subscriptions = merchantMiningService.subscriptionReport();
                concentration = merchantMiningService.concentration();
            } catch (Exception ex) {
                subscriptions = degradedModule("merchants", ex.getMessage());
                concentration = Map.of("degraded", true);
            }
        } else {
            subscriptions = Map.of();
            concentration = Map.of();
        }

        List<Map<String, Object>> cards = CombinedInsightBuilder.build(
                profile, trends, forecast, subscriptions, concentration);
        return new CombinedInsightContext(profile, trends, forecast, subscriptions, concentration, cards);
    }

    private Map<String, Object> loadProfile() {
        if (!features.getProfile().isEnabled()) {
            return Map.of("userType", "balanced", "userTypeExplanation", "Profile module disabled", "dimensions", List.of());
        }
        try (AnalyticsTiming.TimedCall ignored = AnalyticsTiming.start("advisor.profile", ADVISOR_PROFILE_BUDGET_MS)) {
            return profileService.currentProfile();
        } catch (Exception ex) {
            return degradedModule("profile", ex.getMessage());
        }
    }

    private Map<String, Object> loadTrends(int year) {
        if (!features.getForecast().isEnabled()) {
            return Map.of();
        }
        try (AnalyticsTiming.TimedCall ignored = AnalyticsTiming.start("advisor.trends", ADVISOR_TREND_BUDGET_MS)) {
            return trendAnalysisService.trends(year - 1, year);
        } catch (Exception ex) {
            return degradedModule("trends", ex.getMessage());
        }
    }

    private Map<String, Object> loadForecast(int year) {
        if (!features.getForecast().isEnabled()) {
            return Map.of();
        }
        try (AnalyticsTiming.TimedCall ignored = AnalyticsTiming.start("advisor.forecast", ADVISOR_FORECAST_BUDGET_MS)) {
            return forecastService.forecast(year, "base");
        } catch (Exception ex) {
            return degradedModule("forecast", ex.getMessage());
        }
    }

    private static Map<String, Object> degradedModule(String module, String detail) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("degraded", true);
        out.put("module", module);
        out.put("warning", "Module unavailable — showing partial advisor results");
        out.put("detail", detail == null ? "" : detail);
        return out;
    }
}

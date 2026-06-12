package com.finsight.application.analytics;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only NL advisor facade — answers from validated profile/forecast/metrics APIs only (no direct SQL).
 */
@Service
public class LocalAiAdvisorService {

    private final FinancialProfileService profileService;
    private final ForecastService forecastService;
    private final TrendAnalysisService trendAnalysisService;

    public LocalAiAdvisorService(FinancialProfileService profileService,
                                   ForecastService forecastService,
                                   TrendAnalysisService trendAnalysisService) {
        this.profileService = profileService;
        this.forecastService = forecastService;
        this.trendAnalysisService = trendAnalysisService;
    }

    public Map<String, Object> ask(String question) throws Exception {
        String q = question == null ? "" : question.toLowerCase();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("question", question);

        if (q.contains("profile") || q.contains("画像")) {
            Map<String, Object> profile = profileService.currentProfile();
            out.put("answer", "Overall profile score is " + profile.get("overallScore")
                    + " (" + profile.get("userType") + ").");
            out.put("evidenceRefs", List.of(Map.of("source", "profile", "ref", "overall")));
            out.put("data", profile);
            return out;
        }
        if (q.contains("forecast") || q.contains("预测")) {
            int year = java.time.YearMonth.now().getYear();
            Map<String, Object> forecast = forecastService.forecast(year, "base");
            out.put("answer", "Base forecast net for " + year + " is " + forecast.get("yearNet") + ".");
            out.put("evidenceRefs", List.of(Map.of("source", "forecast", "ref", String.valueOf(year))));
            out.put("data", forecast);
            return out;
        }
        if (q.contains("trend") || q.contains("趋势")) {
            int y = java.time.YearMonth.now().getYear();
            Map<String, Object> trends = trendAnalysisService.trends(y - 1, y);
            out.put("answer", "Top category shifts are listed in trend analysis.");
            out.put("evidenceRefs", List.of(Map.of("source", "trends", "ref", y - 1 + "-" + y)));
            out.put("data", trends);
            return out;
        }

        out.put("answer", "I can answer questions about profile, forecast, and trends using verified metrics only.");
        out.put("evidenceRefs", List.of());
        return out;
    }
}

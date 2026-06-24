package com.finsight.web.api.analytics;

import com.finsight.application.analytics.LocalAiAdvisorService;
import com.finsight.application.analytics.MerchantMiningService;
import com.finsight.application.analytics.RecommendationService;
import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.config.FeatureFlagService;
import com.finsight.web.api.dto.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/advisor")
public class AdvisorApiController {

    private final RecommendationService recommendationService;
    private final LocalAiAdvisorService aiAdvisorService;
    private final MerchantMiningService merchantMiningService;
    private final AuthenticationFacade authenticationFacade;
    private final FeatureFlagService featureFlags;

    public AdvisorApiController(RecommendationService recommendationService,
                                LocalAiAdvisorService aiAdvisorService,
                                MerchantMiningService merchantMiningService,
                                AuthenticationFacade authenticationFacade,
                                FeatureFlagService featureFlags) {
        this.recommendationService = recommendationService;
        this.aiAdvisorService = aiAdvisorService;
        this.merchantMiningService = merchantMiningService;
        this.authenticationFacade = authenticationFacade;
        this.featureFlags = featureFlags;
    }

    @GetMapping("/recommendations")
    public CommonResult recommendations() throws Exception {
        featureFlags.requireAdvisor();
        return CommonResult.success(recommendationService.topRecommendations(userKey()));
    }

    @PostMapping("/feedback")
    public CommonResult feedback(@RequestBody Map<String, String> body) {
        featureFlags.requireAdvisor();
        recommendationService.feedback(userKey(), body.get("cardId"), body.get("action"));
        return CommonResult.success(Map.of("ok", true));
    }

    @PostMapping("/ask")
    public CommonResult ask(@RequestBody Map<String, String> body) throws Exception {
        featureFlags.requireLocalAi();
        return CommonResult.success(aiAdvisorService.ask(body.get("question")));
    }

    @PostMapping("/merchants/refresh")
    public CommonResult refreshMerchants() {
        featureFlags.requireMerchantMining();
        return CommonResult.success(merchantMiningService.refreshProfiles());
    }

    @GetMapping("/merchants/subscriptions")
    public CommonResult subscriptions(
            @RequestParam(value = "transactionDateStartStr", required = false) String startStr,
            @RequestParam(value = "transactionDateEndStr", required = false) String endStr) {
        featureFlags.requireMerchantMining();
        return CommonResult.success(merchantMiningService.subscriptionReport(startStr, endStr));
    }

    @GetMapping("/merchants/concentration")
    public CommonResult merchantConcentration() {
        featureFlags.requireMerchantMining();
        return CommonResult.success(merchantMiningService.concentration());
    }

    @GetMapping("/merchants/drift")
    public CommonResult merchantDrift(@RequestParam(defaultValue = "0") int year) {
        featureFlags.requireMerchantMining();
        int targetYear = year > 0 ? year : java.time.Year.now().getValue();
        return CommonResult.success(merchantMiningService.drift(targetYear));
    }

    private String userKey() {
        String user = authenticationFacade.getUserName();
        return user == null || user.isBlank() ? "_anonymous" : user;
    }
}

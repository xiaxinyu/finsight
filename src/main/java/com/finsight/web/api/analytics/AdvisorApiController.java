package com.finsight.web.api.analytics;

import com.finsight.application.analytics.LocalAiAdvisorService;
import com.finsight.application.analytics.MerchantMiningService;
import com.finsight.application.analytics.RecommendationService;
import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.web.api.dto.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/advisor")
public class AdvisorApiController {

    private final RecommendationService recommendationService;
    private final LocalAiAdvisorService aiAdvisorService;
    private final MerchantMiningService merchantMiningService;
    private final AuthenticationFacade authenticationFacade;

    public AdvisorApiController(RecommendationService recommendationService,
                                LocalAiAdvisorService aiAdvisorService,
                                MerchantMiningService merchantMiningService,
                                AuthenticationFacade authenticationFacade) {
        this.recommendationService = recommendationService;
        this.aiAdvisorService = aiAdvisorService;
        this.merchantMiningService = merchantMiningService;
        this.authenticationFacade = authenticationFacade;
    }

    @GetMapping("/recommendations")
    public CommonResult recommendations() throws Exception {
        return CommonResult.success(recommendationService.topRecommendations(userKey()));
    }

    @PostMapping("/feedback")
    public CommonResult feedback(@RequestBody Map<String, String> body) {
        recommendationService.feedback(userKey(), body.get("cardId"), body.get("action"));
        return CommonResult.success(Map.of("ok", true));
    }

    @PostMapping("/ask")
    public CommonResult ask(@RequestBody Map<String, String> body) throws Exception {
        return CommonResult.success(aiAdvisorService.ask(body.get("question")));
    }

    @PostMapping("/merchants/refresh")
    public CommonResult refreshMerchants() {
        return CommonResult.success(merchantMiningService.refreshProfiles());
    }

    @GetMapping("/merchants/subscriptions")
    public CommonResult subscriptions() {
        return CommonResult.success(merchantMiningService.subscriptions());
    }

    private String userKey() {
        String user = authenticationFacade.getUserName();
        return user == null || user.isBlank() ? "_anonymous" : user;
    }
}

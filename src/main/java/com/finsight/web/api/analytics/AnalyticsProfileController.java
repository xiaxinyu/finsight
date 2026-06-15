package com.finsight.web.api.analytics;

import com.finsight.application.analytics.FinancialProfileService;
import com.finsight.application.config.FeatureFlagService;
import com.finsight.web.api.dto.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsProfileController {

    private final FinancialProfileService profileService;
    private final FeatureFlagService featureFlags;

    public AnalyticsProfileController(FinancialProfileService profileService,
                                      FeatureFlagService featureFlags) {
        this.profileService = profileService;
        this.featureFlags = featureFlags;
    }

    @GetMapping("/profile")
    public CommonResult profile() throws Exception {
        featureFlags.requireProfile();
        return CommonResult.success(profileService.currentProfile());
    }

    @GetMapping("/profile/history")
    public CommonResult profileHistory(@RequestParam String from,
                                       @RequestParam String to,
                                       @RequestParam(required = false) String dimension) {
        featureFlags.requireProfile();
        return CommonResult.success(profileService.history(from, to, dimension));
    }
}

package com.finsight.web.api.analytics;

import com.finsight.application.analytics.FinancialProfileService;
import com.finsight.web.api.dto.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsProfileController {

    private final FinancialProfileService profileService;

    public AnalyticsProfileController(FinancialProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public CommonResult profile() throws Exception {
        return CommonResult.success(profileService.currentProfile());
    }

    @GetMapping("/profile/history")
    public CommonResult profileHistory(@RequestParam String from, @RequestParam String to) {
        return CommonResult.success(profileService.history(from, to));
    }
}

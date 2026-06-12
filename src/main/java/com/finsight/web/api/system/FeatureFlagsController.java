package com.finsight.web.api.system;

import com.finsight.application.config.FeatureFlagService;
import com.finsight.web.api.dto.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/features")
public class FeatureFlagsController {

    private final FeatureFlagService featureFlagService;

    public FeatureFlagsController(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @GetMapping
    public CommonResult flags() {
        return CommonResult.success(featureFlagService.snapshot());
    }
}

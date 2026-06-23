package com.finsight.application.analytics;

import com.finsight.application.classification.ConfigVersionService;
import org.springframework.stereotype.Component;

@Component
public class ConfigVersionBump {

    private final ConfigVersionService configVersionService;

    public ConfigVersionBump(ConfigVersionService configVersionService) {
        this.configVersionService = configVersionService;
    }

    public void bumpMetricRefresh() {
        configVersionService.bumpMetricRefresh();
    }

    public void bumpRuleSet() {
        configVersionService.bumpRuleSet();
    }

    public void bumpTaxonomy() {
        configVersionService.bumpTaxonomy();
    }
}

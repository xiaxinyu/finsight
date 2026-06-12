package com.finsight.web.api.system;

import com.finsight.application.analytics.FinancialProfileService;
import com.finsight.application.config.FeatureDisabledException;
import com.finsight.application.config.FeatureFlagService;
import com.finsight.web.api.analytics.AnalyticsProfileController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {FeatureFlagsController.class, AnalyticsProfileController.class})
@AutoConfigureMockMvc(addFilters = false)
class FeatureFlagsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeatureFlagService featureFlagService;

    @MockBean
    private FinancialProfileService profileService;

    @Test
    void featuresEndpoint_returnsConfiguredSnapshot() throws Exception {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("profile", false);
        snapshot.put("advisor", true);
        snapshot.put("localAi", false);
        when(featureFlagService.snapshot()).thenReturn(snapshot);

        mockMvc.perform(get("/api/v1/features"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20000))
                .andExpect(jsonPath("$.data.profile").value(false))
                .andExpect(jsonPath("$.data.advisor").value(true))
                .andExpect(jsonPath("$.data.localAi").value(false));
    }

    @Test
    void profileApi_returns404WhenFlagDisabled() throws Exception {
        doThrow(new FeatureDisabledException("profile")).when(featureFlagService).requireProfile();

        mockMvc.perform(get("/api/v1/analytics/profile"))
                .andExpect(status().isNotFound());
    }
}

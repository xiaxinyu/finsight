package com.finsight.web.api.analytics;

import com.finsight.application.analytics.FinancialProfileService;
import com.finsight.application.config.FeatureFlagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileMaterializationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FinancialProfileService profileService;

    @MockBean
    private FeatureFlagService featureFlagService;

    @Test
    void getProfile_readsMaterializedOnly() throws Exception {
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("overallScore", 80);
        stored.put("materialized", true);
        stored.put("stale", false);
        stored.put("dimensions", List.of());
        when(profileService.currentProfile()).thenReturn(stored);

        mockMvc.perform(get("/api/v1/analytics/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.materialized").value(true))
                .andExpect(jsonPath("$.data.overallScore").value(80));

        verify(profileService, times(1)).currentProfile();
    }

    @Test
    void postRefresh_triggersExplicitCompute() throws Exception {
        Map<String, Object> refreshed = new LinkedHashMap<>();
        refreshed.put("refreshed", true);
        refreshed.put("overallScore", 75);
        refreshed.put("dimensions", List.of());
        when(profileService.refreshProfileCurrent()).thenReturn(refreshed);

        mockMvc.perform(post("/api/v1/analytics/profile/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshed").value(true));

        verify(profileService, times(1)).refreshProfileCurrent();
    }
}

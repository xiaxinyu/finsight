package com.finsight.web.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:prodsec;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=secure-test-pass",
        "account.des-sign-key=secure-prod-sign-key",
        "spring.flyway.enabled=false",
        "finsight.security.actuator-public=false",
        "finsight.security.csrf-enabled=true",
})
class ProdActuatorSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void actuatorHealthRedirectsToLoginWhenNotPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/app/login"));
    }

    @Test
    @WithMockUser
    void encryptEndpointDeniedInProdEvenWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/encrypt/bcrypt").param("raw", "secret"))
                .andExpect(status().isForbidden());
    }
}

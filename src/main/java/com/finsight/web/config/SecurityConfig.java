package com.finsight.web.config;

import com.finsight.application.config.FinsightFeatureProperties;
import com.finsight.common.security.SecurityRoles;
import com.finsight.web.security.ApiAccessDeniedHandler;
import com.finsight.web.security.ApiAuthenticationEntryPoint;
import com.finsight.web.security.LoginAttemptTracker;
import com.finsight.web.security.LoginRateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<LoginRateLimitFilter> loginRateLimitFilterRegistration(
            LoginAttemptTracker tracker,
            FinsightFeatureProperties features) {
        FilterRegistrationBean<LoginRateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new LoginRateLimitFilter(
                tracker,
                features.getSecurity().getLoginMaxAttempts(),
                features.getSecurity().getLoginLockoutSeconds()));
        registration.addUrlPatterns("/authentication/form");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationManager authenticationManager,
                                                   FinsightFeatureProperties features,
                                                   Environment environment,
                                                   LoginAttemptTracker loginAttemptTracker) throws Exception {
        http.headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .contentTypeOptions(content -> {})
                .xssProtection(xss -> {})
        );
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            http.headers(headers -> headers.httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)));
        }

        if (features.getSecurity().isCsrfEnabled()) {
            CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
            tokenRepository.setCookiePath("/");
            CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
            requestHandler.setCsrfRequestAttributeName("_csrf");
            http.csrf(csrf -> csrf
                    .csrfTokenRepository(tokenRepository)
                    .csrfTokenRequestHandler(requestHandler)
                    .ignoringRequestMatchers("/app/**"));
        } else {
            http.csrf(csrf -> csrf.disable());
        }

        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(new ApiAuthenticationEntryPoint())
                .accessDeniedHandler(new ApiAccessDeniedHandler()));

        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers(
                    "/oauth/**",
                    "/login/**",
                    "/logout/**",
                    "/plugins/**",
                    "/login-error.json",
                    "/app/**",
                    "/api/v1/auth/csrf"
            ).permitAll();
            if (features.getSecurity().isActuatorPublic()) {
                auth.requestMatchers("/actuator/health").permitAll();
            }
            auth.requestMatchers("/actuator/**").authenticated();

            auth.requestMatchers("/api/v1/users/**").hasRole(SecurityRoles.ADMIN);
            auth.requestMatchers("/api/v1/maintenance/**").hasRole(SecurityRoles.ADMIN);
            auth.requestMatchers(HttpMethod.POST, "/api/v1/cards").authenticated();
            auth.requestMatchers(HttpMethod.PUT, "/api/v1/cards/**").authenticated();
            auth.requestMatchers(HttpMethod.DELETE, "/api/v1/cards/**").authenticated();
            auth.requestMatchers(HttpMethod.POST, "/api/v1/classification/**").hasRole(SecurityRoles.ADMIN);
            auth.requestMatchers(HttpMethod.PUT, "/api/v1/classification/**").hasRole(SecurityRoles.ADMIN);
            auth.requestMatchers(HttpMethod.DELETE, "/api/v1/classification/**").hasRole(SecurityRoles.ADMIN);
            auth.requestMatchers("/api/v1/consume/rules/**").hasRole(SecurityRoles.ADMIN);
            auth.requestMatchers(HttpMethod.POST, "/api/v1/consume/categories/**").hasRole(SecurityRoles.ADMIN);
            auth.requestMatchers(HttpMethod.PUT, "/api/v1/consume/categories/**").hasRole(SecurityRoles.ADMIN);
            auth.requestMatchers(HttpMethod.DELETE, "/api/v1/consume/categories/**").hasRole(SecurityRoles.ADMIN);

            if (environment.acceptsProfiles(Profiles.of("prod"))) {
                auth.requestMatchers("/encrypt/**").denyAll();
            } else {
                auth.requestMatchers("/encrypt/**").authenticated();
            }
            auth.anyRequest().authenticated();
        });

        http.authenticationManager(authenticationManager);
        http.sessionManagement(session -> session
                .sessionFixation(fix -> fix.migrateSession())
        );

        http.formLogin(form -> form
                .loginPage("/app/login")
                .loginProcessingUrl("/authentication/form")
                .failureHandler((request, response, exception) -> {
                    String clientKey = LoginAttemptTracker.clientKey(request);
                    loginAttemptTracker.recordFailure(clientKey);

                    String code = "BAD_CREDENTIALS";
                    String msg = "Invalid username or password";
                    if (exception instanceof org.springframework.security.authentication.DisabledException) {
                        code = "DISABLED";
                        msg = "Account is disabled";
                    } else if (exception instanceof org.springframework.security.authentication.AccountExpiredException) {
                        code = "ACCOUNT_EXPIRED";
                        msg = "Account has expired";
                    } else if (exception instanceof org.springframework.security.authentication.CredentialsExpiredException) {
                        code = "CREDENTIALS_EXPIRED";
                        msg = "Credentials have expired";
                    } else if (exception instanceof org.springframework.security.authentication.LockedException) {
                        code = "LOCKED";
                        msg = "Account is locked";
                    }
                    String user = request.getParameter("username");
                    org.slf4j.LoggerFactory.getLogger(SecurityConfig.class)
                            .warn("Login failed: user={} code={}", user, code);
                    jakarta.servlet.http.HttpSession sess = request.getSession(true);
                    sess.setAttribute("LOGIN_ERROR_CODE", code);
                    sess.setAttribute("LOGIN_ERROR_MSG", msg);
                    response.sendRedirect("/app/login");
                })
                .successHandler((request, response, authentication) -> {
                    loginAttemptTracker.reset(LoginAttemptTracker.clientKey(request));
                    response.sendRedirect("/app/dashboard");
                })
                .permitAll()
        );

        http.logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/app/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
        );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(encoder);
        provider.setHideUserNotFoundExceptions(true);
        return new ProviderManager(provider);
    }
}

package com.finsight.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
        http.csrf(csrf -> csrf.disable());
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/oauth/**", "/login/**", "/logout/**", "/actuator/**", "/plugins/**", "/encrypt/**", "/login-error.json").permitAll()
                .anyRequest().authenticated()
        );
        http.authenticationManager(authenticationManager);
        http.formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/authentication/form")
                .failureHandler((request, response, exception) -> {
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
                    } else if (exception instanceof org.springframework.security.authentication.BadCredentialsException) {
                        code = "BAD_CREDENTIALS";
                        msg = "Invalid username or password";
                    } else if (exception instanceof org.springframework.security.core.userdetails.UsernameNotFoundException) {
                        code = "NOT_FOUND";
                        msg = "User not found";
                    }
                    String user = request.getParameter("username");
                    log.warn("Login failed: user={} code={} msg={}", user, code, msg);
                    jakarta.servlet.http.HttpSession sess = request.getSession(true);
                    sess.setAttribute("LOGIN_ERROR_CODE", code);
                    sess.setAttribute("LOGIN_ERROR_MSG", msg);
                    response.sendRedirect("/login.html");
                })
                .defaultSuccessUrl("/index.html", true)
                .permitAll()
        );
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
}

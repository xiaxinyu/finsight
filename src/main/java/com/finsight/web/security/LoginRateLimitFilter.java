package com.finsight.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Limits failed login attempts per client IP to slow brute-force attacks.
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    static final String LOGIN_PROCESSING_URL = "/authentication/form";
    static final String RATE_LIMIT_MSG = "Too many login attempts. Please wait and try again.";

    private final LoginAttemptTracker tracker;
    private final int maxAttempts;
    private final long windowSeconds;

    public LoginRateLimitFilter(LoginAttemptTracker tracker, int maxAttempts, long windowSeconds) {
        this.tracker = tracker;
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !LOGIN_PROCESSING_URL.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String clientKey = LoginAttemptTracker.clientKey(request);
        if (tracker.isBlocked(clientKey, maxAttempts, windowSeconds)) {
            HttpSession session = request.getSession(true);
            session.setAttribute("LOGIN_ERROR_CODE", "RATE_LIMITED");
            session.setAttribute("LOGIN_ERROR_MSG", RATE_LIMIT_MSG);
            response.sendRedirect("/app/login");
            return;
        }
        filterChain.doFilter(request, response);
    }
}

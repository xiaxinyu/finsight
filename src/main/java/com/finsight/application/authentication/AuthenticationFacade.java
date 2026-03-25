package com.finsight.application.authentication;

import org.springframework.security.core.Authentication;

/**
 * Application-facing access to the current Spring Security principal (for audit fields, ownership checks, etc.).
 */
public interface AuthenticationFacade {
    Authentication getAuthentication();

    String getUserName();
}

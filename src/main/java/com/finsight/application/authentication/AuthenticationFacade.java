package com.finsight.application.authentication;

import org.springframework.security.core.Authentication;

public interface AuthenticationFacade {
    Authentication getAuthentication();

    String getUserName();
}

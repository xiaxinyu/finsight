package com.finsight.application.authentication.impl;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.core.AppException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Delegates to {@link org.springframework.security.core.context.SecurityContextHolder};
 * {@link #getUserName()} requires an authenticated context.
 */
@Service
public class AuthenticationFacadeImpl implements AuthenticationFacade {

    @Override
    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Override
    public String getUserName() {
        Authentication authentication = getAuthentication();
        if (Objects.isNull(authentication)) {
            throw new AppException("error.system.error");
        }
        return authentication.getName();
    }
}

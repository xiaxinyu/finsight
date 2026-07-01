package com.finsight.application.authentication;

import com.finsight.common.exception.AppServiceException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the ledger owner id (Spring Security username) for row-level isolation.
 */
@Component
public class LedgerUserScope {

    public String resolve() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()
                    || auth.getPrincipal() == null
                    || "anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
                return "_anonymous";
            }
            String name = auth.getName();
            return name == null || name.isBlank() ? "_anonymous" : name.trim();
        } catch (Exception ex) {
            return "_anonymous";
        }
    }

    public boolean owns(String createdBy) {
        String owner = resolve();
        if (createdBy == null || createdBy.isBlank()) {
            return "_anonymous".equals(owner);
        }
        return owner.equals(createdBy.trim());
    }

    public void assertOwned(String createdBy) throws AppServiceException {
        if (!owns(createdBy)) {
            throw new AppServiceException("Access denied: resource belongs to another user");
        }
    }
}

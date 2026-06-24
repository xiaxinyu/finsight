package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import org.springframework.stereotype.Service;

/**
 * Invalidates in-process analytics caches after ledger or metric mutations.
 */
@Service
public class AnalyticsCacheInvalidationService {

    private final AnalyticsCacheService cacheService;
    private final AnalyticsCacheKeySupport cacheKeySupport;
    private final AuthenticationFacade authenticationFacade;

    public AnalyticsCacheInvalidationService(AnalyticsCacheService cacheService,
                                             AnalyticsCacheKeySupport cacheKeySupport,
                                             AuthenticationFacade authenticationFacade) {
        this.cacheService = cacheService;
        this.cacheKeySupport = cacheKeySupport;
        this.authenticationFacade = authenticationFacade;
    }

    public void invalidateForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        cacheService.invalidateProfile(cacheKeySupport.profileKey(userId));
        cacheService.invalidateAdvisor(cacheKeySupport.advisorKey(userId));
        cacheService.invalidateForecastsForUser(userId);
    }

    public void invalidateCurrentUser() {
        String user = authenticationFacade.getUserName();
        invalidateForUser(user == null || user.isBlank() ? "_anonymous" : user);
    }
}

package com.finsight.application.authentication;

import com.finsight.common.exception.AppServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LedgerUserScopeTest {

    private final LedgerUserScope scope = new LedgerUserScope();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolve_returnsAuthenticatedUsername() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("xiaxinyu", "n/a", List.of()));
        assertEquals("xiaxinyu", scope.resolve());
    }

    @Test
    void owns_matchesCreatedBy() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("xiaxinyu", "n/a", List.of()));
        assertTrue(scope.owns("xiaxinyu"));
        assertFalse(scope.owns("other"));
    }

    @Test
    void assertOwned_rejectsForeignRows() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("xiaxinyu", "n/a", List.of()));
        assertDoesNotThrow(() -> scope.assertOwned("xiaxinyu"));
        assertThrows(AppServiceException.class, () -> scope.assertOwned("admin"));
    }
}

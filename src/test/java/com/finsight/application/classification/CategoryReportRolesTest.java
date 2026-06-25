package com.finsight.application.classification;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CategoryReportRolesTest {

    @Test
    void normalize_acceptsKnownRoles() {
        assertEquals("budget", CategoryReportRoles.normalize("Budget"));
        assertEquals("investment", CategoryReportRoles.normalize("investment"));
    }

    @Test
    void normalize_blankReturnsNull() {
        assertNull(CategoryReportRoles.normalize(null));
        assertNull(CategoryReportRoles.normalize("  "));
    }

    @Test
    void normalize_rejectsUnknown() {
        assertThrows(ResponseStatusException.class, () -> CategoryReportRoles.normalize("salary"));
    }
}

package com.finsight.application.classification;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

/** Allowed {@code cls_category.report_role} values (aligned with finance semantic contract). */
public final class CategoryReportRoles {

    public static final Set<String> ALLOWED = Set.of(
            "income",
            "refund",
            "budget",
            "cashflow",
            "investment",
            "liability",
            "asset",
            "transfer",
            "other");

    private CategoryReportRoles() {
    }

    public static String normalize(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String role = raw.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED.contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report_role: " + raw);
        }
        return role;
    }
}

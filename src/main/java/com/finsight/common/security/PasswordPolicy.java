package com.finsight.common.security;

import org.apache.commons.lang3.StringUtils;

/**
 * Minimum password rules for local accounts.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;

    private PasswordPolicy() {
    }

    public static void validate(String rawPassword) {
        if (StringUtils.isBlank(rawPassword)) {
            throw new IllegalArgumentException("Password is required");
        }
        String p = rawPassword.trim();
        if (p.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_LENGTH + " characters");
        }
        if (!p.matches(".*[A-Za-z].*") || !p.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one letter and one digit");
        }
    }
}

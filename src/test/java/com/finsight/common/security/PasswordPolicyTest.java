package com.finsight.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    @Test
    void acceptsStrongPassword() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("Xiaxinyu1"));
    }

    @Test
    void rejectsShortOrTrivial() {
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validate("123456"));
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validate("abcdefgh"));
    }
}

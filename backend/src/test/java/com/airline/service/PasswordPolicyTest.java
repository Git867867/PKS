package com.airline.service;

import com.airline.error.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    @Test
    void acceptsPasswordWithAllGroups() {
        assertDoesNotThrow(() -> PasswordPolicy.check("Adm1!Airline26"));
    }

    @Test
    void rejectsShortPassword() {
        assertThrows(BadRequestException.class, () -> PasswordPolicy.check("Aa1!a"));
    }

    @Test
    void rejectsPasswordWithoutSpecialCharacter() {
        assertThrows(BadRequestException.class, () -> PasswordPolicy.check("Adm1Airline26"));
    }

    @Test
    void temporaryPasswordMeetsPolicy() {
        assertDoesNotThrow(() -> PasswordPolicy.check(PasswordPolicy.temporary()));
    }
}

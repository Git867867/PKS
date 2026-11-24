package com.airline.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class Auth {

    private Auth() {
    }

    public static CurrentUser current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CurrentUser user) {
            return user;
        }
        return null;
    }

    public static CurrentUser require() {
        CurrentUser user = current();
        if (user == null) {
            throw new com.airline.error.UnauthorizedException("Требуется вход.");
        }
        return user;
    }
}

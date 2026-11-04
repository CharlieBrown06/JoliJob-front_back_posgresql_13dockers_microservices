package com.example.jolijob.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static String getCurrentUserEmail() {
        return getAuthentication().getName();
    }

    public static UUID getCurrentUserId() {
        Jwt jwt = (Jwt) getAuthentication().getPrincipal();
        return UUID.fromString(jwt.getClaimAsString("userId"));
    }

    public static String getCurrentUserRole() {
        Jwt jwt = (Jwt) getAuthentication().getPrincipal();
        return jwt.getClaimAsString("role");
    }

    private static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}

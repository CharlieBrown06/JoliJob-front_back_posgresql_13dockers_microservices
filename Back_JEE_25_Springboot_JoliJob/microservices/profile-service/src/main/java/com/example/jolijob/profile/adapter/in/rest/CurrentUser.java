package com.example.jolijob.profile.adapter.in.rest;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

final class CurrentUser {
    private CurrentUser() {
    }

    static UUID id(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new BadCredentialsException("JWT subject must be a UUID", exception);
        } catch (NullPointerException exception) {
            throw new BadCredentialsException("JWT subject is required", exception);
        }
    }
}

package com.example.jolijob.application.domain;

import java.util.UUID;

public record Actor(UUID userId, String email, String role) {
}

package com.example.jolijob.application.adapter.in.web;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ApplicationRequest(
        @NotNull UUID jobOfferId,
        String coverLetter,
        String cvUrl
) {
}

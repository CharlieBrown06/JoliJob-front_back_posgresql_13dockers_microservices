package com.example.jolijob.application.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ApplicationRequest(
        @NotNull UUID jobOfferId,
        String coverLetter,
        String cvUrl
) {}

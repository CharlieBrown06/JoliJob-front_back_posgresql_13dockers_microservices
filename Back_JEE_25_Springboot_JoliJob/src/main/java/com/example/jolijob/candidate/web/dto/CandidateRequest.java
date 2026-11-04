package com.example.jolijob.candidate.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CandidateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        String city,
        String country,
        String jobTitle,
        String summary,
        String cvUrl,
        String linkedinUrl,
        String portfolioUrl,
        LocalDate birthDate,
        boolean openToRemote,
        boolean openToRelocation
) {}

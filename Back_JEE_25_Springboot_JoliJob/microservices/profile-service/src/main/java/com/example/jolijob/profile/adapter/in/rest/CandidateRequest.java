package com.example.jolijob.profile.adapter.in.rest;

import com.example.jolijob.profile.domain.CandidateDetails;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CandidateRequest(
        @NotBlank String firstName, @NotBlank String lastName, String phone, String city,
        String country, String jobTitle, String summary, String cvUrl, String linkedinUrl,
        String portfolioUrl, LocalDate birthDate, boolean openToRemote, boolean openToRelocation
) {
    CandidateDetails toDetails() {
        return new CandidateDetails(firstName, lastName, phone, city, country, jobTitle,
                summary, cvUrl, linkedinUrl, portfolioUrl, birthDate, openToRemote, openToRelocation);
    }
}

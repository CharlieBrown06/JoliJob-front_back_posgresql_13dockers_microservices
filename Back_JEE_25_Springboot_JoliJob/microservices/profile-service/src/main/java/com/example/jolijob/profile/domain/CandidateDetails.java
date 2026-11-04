package com.example.jolijob.profile.domain;

import java.time.LocalDate;

public record CandidateDetails(
        String firstName, String lastName, String phone, String city, String country,
        String jobTitle, String summary, String cvUrl, String linkedinUrl, String portfolioUrl,
        LocalDate birthDate, boolean openToRemote, boolean openToRelocation
) {
    public CandidateDetails {
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Candidate first and last names are required");
        }
    }
}

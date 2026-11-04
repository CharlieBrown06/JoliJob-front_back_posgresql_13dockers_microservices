package com.example.jolijob.profile.adapter.in.rest;

import com.example.jolijob.profile.domain.CandidateDetails;
import com.example.jolijob.profile.domain.CandidateProfile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CandidateResponse(
        UUID id, UUID userId, String firstName, String lastName, String phone, String city,
        String country, String jobTitle, String summary, String cvUrl, String linkedinUrl,
        String portfolioUrl, LocalDate birthDate, boolean openToRemote, boolean openToRelocation,
        Instant createdAt
) {
    static CandidateResponse from(CandidateProfile profile) {
        CandidateDetails details = profile.details();
        return new CandidateResponse(profile.id(), profile.userId(), details.firstName(),
                details.lastName(), details.phone(), details.city(), details.country(), details.jobTitle(),
                details.summary(), details.cvUrl(), details.linkedinUrl(), details.portfolioUrl(),
                details.birthDate(), details.openToRemote(), details.openToRelocation(), profile.createdAt());
    }
}

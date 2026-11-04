package com.example.jolijob.candidate.web.dto;

import com.example.jolijob.candidate.domain.Candidate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CandidateResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
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
        boolean openToRelocation,
        Instant createdAt
) {
    public static CandidateResponse from(Candidate c) {
        return new CandidateResponse(
                c.getId(), c.getUserId(), c.getFirstName(), c.getLastName(),
                c.getPhone(), c.getCity(), c.getCountry(), c.getJobTitle(),
                c.getSummary(), c.getCvUrl(), c.getLinkedinUrl(), c.getPortfolioUrl(),
                c.getBirthDate(), c.isOpenToRemote(), c.isOpenToRelocation(), c.getCreatedAt()
        );
    }
}

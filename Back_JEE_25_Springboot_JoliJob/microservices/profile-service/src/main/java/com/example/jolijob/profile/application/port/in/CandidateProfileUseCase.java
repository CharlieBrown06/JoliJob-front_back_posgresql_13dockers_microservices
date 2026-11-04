package com.example.jolijob.profile.application.port.in;

import com.example.jolijob.profile.domain.CandidateDetails;
import com.example.jolijob.profile.domain.CandidateProfile;

import java.util.UUID;

public interface CandidateProfileUseCase {
    CandidateProfile create(UUID userId, CandidateDetails details);
    CandidateProfile update(UUID userId, CandidateDetails details);
    CandidateProfile getByUserId(UUID userId);
    CandidateProfile getById(UUID candidateId);
}

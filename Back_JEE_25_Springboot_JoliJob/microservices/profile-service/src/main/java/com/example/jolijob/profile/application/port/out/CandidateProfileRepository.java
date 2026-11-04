package com.example.jolijob.profile.application.port.out;

import com.example.jolijob.profile.domain.CandidateProfile;

import java.util.Optional;
import java.util.UUID;

public interface CandidateProfileRepository {
    Optional<CandidateProfile> findByUserId(UUID userId);
    Optional<CandidateProfile> findById(UUID id);
    CandidateProfile save(CandidateProfile profile);
}

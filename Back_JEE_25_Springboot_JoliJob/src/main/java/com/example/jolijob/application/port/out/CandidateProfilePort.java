package com.example.jolijob.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface CandidateProfilePort {
    Optional<CandidateProfile> findByUserId(UUID userId);

    record CandidateProfile(UUID id, String cvUrl) {}
}

package com.example.jolijob.profile.application.port.out;

import com.example.jolijob.profile.domain.CompanyProfile;

import java.util.Optional;
import java.util.UUID;

public interface CompanyProfileRepository {
    Optional<CompanyProfile> findByUserId(UUID userId);
    Optional<CompanyProfile> findById(UUID id);
    CompanyProfile save(CompanyProfile profile);
}

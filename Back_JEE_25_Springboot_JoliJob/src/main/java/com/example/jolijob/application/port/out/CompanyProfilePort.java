package com.example.jolijob.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface CompanyProfilePort {
    Optional<CompanyProfile> findByUserId(UUID userId);
    Optional<CompanyProfile> findById(UUID companyId);

    record CompanyProfile(UUID id, String contactEmail) {}
}

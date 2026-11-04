package com.example.jolijob.joboffer.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface CompanyPort {
    Optional<Company> findByUserId(UUID userId);

    record Company(UUID id, UUID userId, String name) {}
}

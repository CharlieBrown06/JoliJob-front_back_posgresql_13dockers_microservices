package com.example.jolijob.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface JobOfferPort {
    Optional<JobOfferSummary> findById(UUID jobOfferId);

    record JobOfferSummary(UUID id, boolean active, UUID companyId, String title) {}
}

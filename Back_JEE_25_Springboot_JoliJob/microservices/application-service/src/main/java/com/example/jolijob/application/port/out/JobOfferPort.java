package com.example.jolijob.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface JobOfferPort {
    Optional<JobOffer> findById(UUID id);

    record JobOffer(UUID id, boolean active, UUID companyId, String title) {
    }
}

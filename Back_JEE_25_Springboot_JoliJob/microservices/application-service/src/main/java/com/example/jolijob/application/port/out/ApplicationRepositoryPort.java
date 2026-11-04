package com.example.jolijob.application.port.out;

import com.example.jolijob.application.domain.Application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepositoryPort {
    Application save(Application application);

    Optional<Application> findById(UUID id);

    void lockById(UUID id);

    boolean existsByJobOfferIdAndCandidateId(UUID jobOfferId, UUID candidateId);

    List<Application> findByCandidateId(UUID candidateId);

    List<Application> findByJobOfferId(UUID jobOfferId);
}

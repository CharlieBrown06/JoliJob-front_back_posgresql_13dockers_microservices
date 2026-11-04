package com.example.jolijob.application.repository;

import com.example.jolijob.application.domain.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    List<Application> findByCandidateId(UUID candidateId);
    List<Application> findByJobOfferId(UUID jobOfferId);
    boolean existsByJobOfferIdAndCandidateId(UUID jobOfferId, UUID candidateId);
}

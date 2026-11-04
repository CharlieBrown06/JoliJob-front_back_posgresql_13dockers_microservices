package com.example.jolijob.joboffer.service;

import com.example.jolijob.company.repository.CompanyRepository;
import com.example.jolijob.joboffer.domain.JobOffer;
import com.example.jolijob.joboffer.kafka.JobOfferEventPublisher;
import com.example.jolijob.joboffer.repository.JobOfferRepository;
import com.example.jolijob.joboffer.web.dto.JobOfferRequest;
import com.example.jolijob.joboffer.web.dto.JobOfferResponse;
import com.example.jolijob.shared.event.JobOfferEventType;
import com.example.jolijob.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobOfferService {

    private final JobOfferRepository jobOfferRepository;
    private final CompanyRepository companyRepository;
    private final JobOfferEventPublisher eventPublisher;

    @Transactional
    public JobOfferResponse create(JobOfferRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        var company = companyRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Créez votre profil entreprise d'abord"));
        JobOffer offer = new JobOffer();
        offer.setCompanyId(company.getId());
        offer.setCompanyName(company.getName());
        applyRequest(offer, request);
        offer = jobOfferRepository.save(offer);
        eventPublisher.publish(offer, JobOfferEventType.CREATED);
        return JobOfferResponse.from(offer);
    }

    @Transactional
    public JobOfferResponse update(UUID offerId, JobOfferRequest request) {
        JobOffer offer = getOwnedOffer(offerId);
        applyRequest(offer, request);
        offer = jobOfferRepository.save(offer);
        eventPublisher.publish(offer, JobOfferEventType.UPDATED);
        return JobOfferResponse.from(offer);
    }

    @Transactional
    public void delete(UUID offerId) {
        JobOffer offer = getOwnedOffer(offerId);
        offer.setActive(false);
        jobOfferRepository.save(offer);
        eventPublisher.publish(offer, JobOfferEventType.DELETED);
    }

    @Transactional(readOnly = true)
    public JobOfferResponse getById(UUID offerId) {
        return jobOfferRepository.findById(offerId)
                .map(JobOfferResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Offre introuvable : " + offerId));
    }

    @Transactional(readOnly = true)
    public List<JobOfferResponse> getMyOffers() {
        UUID userId = SecurityUtils.getCurrentUserId();
        var company = companyRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Profil entreprise introuvable"));
        return jobOfferRepository.findByCompanyIdAndActiveTrue(company.getId())
                .stream().map(JobOfferResponse::from).toList();
    }

    private JobOffer getOwnedOffer(UUID offerId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        var company = companyRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Profil entreprise introuvable"));
        JobOffer offer = jobOfferRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offre introuvable : " + offerId));
        if (!offer.getCompanyId().equals(company.getId())) {
            throw new SecurityException("Accès refusé à cette offre");
        }
        return offer;
    }

    private void applyRequest(JobOffer offer, JobOfferRequest req) {
        offer.setTitle(req.title());
        offer.setDescription(req.description());
        offer.setJobCategory(req.jobCategory());
        offer.setSpecialty(req.specialty());
        offer.setContractType(req.contractType());
        offer.setRemote(req.remote());
        offer.setCountry(req.country());
        offer.setRegion(req.region());
        offer.setDepartment(req.department());
        offer.setCity(req.city());
        offer.setSalaryMin(req.salaryMin());
        offer.setSalaryMax(req.salaryMax());
        offer.setStartDate(req.startDate());
        offer.setExpiresAt(req.expiresAt());
    }
}

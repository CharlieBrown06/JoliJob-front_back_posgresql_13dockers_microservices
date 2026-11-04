package com.example.jolijob.application.service;

import com.example.jolijob.application.domain.Application;
import com.example.jolijob.application.domain.ApplicationStatus;
import com.example.jolijob.application.port.out.ApplicationEventPort;
import com.example.jolijob.application.port.out.CandidateProfilePort;
import com.example.jolijob.application.port.out.CompanyProfilePort;
import com.example.jolijob.application.port.out.JobOfferPort;
import com.example.jolijob.application.repository.ApplicationRepository;
import com.example.jolijob.application.web.dto.ApplicationRequest;
import com.example.jolijob.application.web.dto.ApplicationResponse;
import com.example.jolijob.application.web.dto.ApplicationStatusRequest;
import com.example.jolijob.shared.event.ApplicationEventType;
import com.example.jolijob.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CandidateProfilePort candidateProfilePort;
    private final CompanyProfilePort companyProfilePort;
    private final JobOfferPort jobOfferPort;
    private final ApplicationEventPort eventPublisher;

    @Transactional
    public ApplicationResponse apply(ApplicationRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        var candidate = candidateProfilePort.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Créez votre profil candidat d'abord"));
        var offer = jobOfferPort.findById(request.jobOfferId())
                .orElseThrow(() -> new IllegalArgumentException("Offre introuvable"));
        if (!offer.active()) {
            throw new IllegalStateException("Cette offre n'est plus active");
        }
        if (applicationRepository.existsByJobOfferIdAndCandidateId(offer.id(), candidate.id())) {
            throw new IllegalStateException("Vous avez déjà postulé à cette offre");
        }

        Application app = new Application();
        app.setJobOfferId(offer.id());
        app.setCandidateId(candidate.id());
        app.setCandidateEmail(SecurityUtils.getCurrentUserEmail());
        app.setCompanyId(offer.companyId());
        app.setJobTitle(offer.title());
        app.setCoverLetter(request.coverLetter());
        app.setCvUrl(request.cvUrl() != null ? request.cvUrl() : candidate.cvUrl());

        companyProfilePort.findById(offer.companyId())
                .ifPresent(company -> app.setCompanyEmail(company.contactEmail()));

        Application savedApplication = applicationRepository.save(app);
        eventPublisher.publish(savedApplication, ApplicationEventType.SUBMITTED);
        return ApplicationResponse.from(savedApplication);
    }

    @Transactional
    public ApplicationResponse updateStatus(UUID applicationId, ApplicationStatusRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        var company = companyProfilePort.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Profil entreprise introuvable"));
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable"));
        if (!app.getCompanyId().equals(company.id())) {
            throw new SecurityException("Accès refusé");
        }
        app.setStatus(request.status());
        app = applicationRepository.save(app);
        eventPublisher.publish(app, ApplicationEventType.STATUS_CHANGED);
        return ApplicationResponse.from(app);
    }

    @Transactional
    public ApplicationResponse withdraw(UUID applicationId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        var candidate = candidateProfilePort.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Profil candidat introuvable"));
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable"));
        if (!app.getCandidateId().equals(candidate.id())) {
            throw new SecurityException("Accès refusé");
        }
        app.setStatus(ApplicationStatus.WITHDRAWN);
        app = applicationRepository.save(app);
        eventPublisher.publish(app, ApplicationEventType.WITHDRAWN);
        return ApplicationResponse.from(app);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyCandidateApplications() {
        UUID userId = SecurityUtils.getCurrentUserId();
        var candidate = candidateProfilePort.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Profil candidat introuvable"));
        return applicationRepository.findByCandidateId(candidate.id())
                .stream().map(ApplicationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getOfferApplications(UUID jobOfferId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        var company = companyProfilePort.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Profil entreprise introuvable"));
        var offer = jobOfferPort.findById(jobOfferId)
                .orElseThrow(() -> new IllegalArgumentException("Offre introuvable"));
        if (!offer.companyId().equals(company.id())) {
            throw new SecurityException("Accès refusé");
        }
        return applicationRepository.findByJobOfferId(jobOfferId)
                .stream().map(ApplicationResponse::from).toList();
    }
}

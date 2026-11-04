package com.example.jolijob.candidate.service;

import com.example.jolijob.candidate.domain.Candidate;
import com.example.jolijob.candidate.repository.CandidateRepository;
import com.example.jolijob.candidate.web.dto.CandidateRequest;
import com.example.jolijob.candidate.web.dto.CandidateResponse;
import com.example.jolijob.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepository candidateRepository;

    @Transactional
    public CandidateResponse createProfile(CandidateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (candidateRepository.existsByUserId(userId)) {
            throw new IllegalStateException("Profil candidat déjà existant");
        }
        Candidate candidate = new Candidate(userId, request.firstName(), request.lastName());
        applyRequest(candidate, request);
        return CandidateResponse.from(candidateRepository.save(candidate));
    }

    @Transactional
    public CandidateResponse updateProfile(CandidateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Candidate candidate = candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Profil candidat introuvable"));
        applyRequest(candidate, request);
        return CandidateResponse.from(candidateRepository.save(candidate));
    }

    @Transactional(readOnly = true)
    public CandidateResponse getMyProfile() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return candidateRepository.findByUserId(userId)
                .map(CandidateResponse::from)
                .orElseThrow(() -> new IllegalStateException("Profil candidat introuvable"));
    }

    @Transactional(readOnly = true)
    public CandidateResponse getById(UUID candidateId) {
        return candidateRepository.findById(candidateId)
                .map(CandidateResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Candidat introuvable : " + candidateId));
    }

    private void applyRequest(Candidate c, CandidateRequest req) {
        c.setFirstName(req.firstName());
        c.setLastName(req.lastName());
        c.setPhone(req.phone());
        c.setCity(req.city());
        c.setCountry(req.country());
        c.setJobTitle(req.jobTitle());
        c.setSummary(req.summary());
        c.setCvUrl(req.cvUrl());
        c.setLinkedinUrl(req.linkedinUrl());
        c.setPortfolioUrl(req.portfolioUrl());
        c.setBirthDate(req.birthDate());
        c.setOpenToRemote(req.openToRemote());
        c.setOpenToRelocation(req.openToRelocation());
    }
}

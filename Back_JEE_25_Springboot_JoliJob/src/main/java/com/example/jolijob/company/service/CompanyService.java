package com.example.jolijob.company.service;

import com.example.jolijob.company.domain.Company;
import com.example.jolijob.company.repository.CompanyRepository;
import com.example.jolijob.company.web.dto.CompanyRequest;
import com.example.jolijob.company.web.dto.CompanyResponse;
import com.example.jolijob.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional
    public CompanyResponse createProfile(CompanyRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (companyRepository.existsByUserId(userId)) {
            throw new IllegalStateException("Profil entreprise déjà existant");
        }
        Company company = new Company(userId, request.name(), request.contactEmail());
        applyRequest(company, request);
        return CompanyResponse.from(companyRepository.save(company));
    }

    @Transactional
    public CompanyResponse updateProfile(CompanyRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Company company = companyRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Profil entreprise introuvable"));
        applyRequest(company, request);
        return CompanyResponse.from(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public CompanyResponse getMyProfile() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return companyRepository.findByUserId(userId)
                .map(CompanyResponse::from)
                .orElseThrow(() -> new IllegalStateException("Profil entreprise introuvable"));
    }

    @Transactional(readOnly = true)
    public CompanyResponse getById(UUID companyId) {
        return companyRepository.findById(companyId)
                .map(CompanyResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Entreprise introuvable : " + companyId));
    }

    private void applyRequest(Company company, CompanyRequest req) {
        company.setName(req.name());
        company.setSiret(req.siret());
        company.setDescription(req.description());
        company.setWebsite(req.website());
        company.setSector(req.sector());
        company.setCountry(req.country());
        company.setCity(req.city());
        company.setLogoUrl(req.logoUrl());
        company.setContactEmail(req.contactEmail());
        company.setContactPhone(req.contactPhone());
    }
}

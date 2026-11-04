package com.example.jolijob.profile.adapter.in.rest;

import com.example.jolijob.profile.application.ProfileNotFoundException;
import com.example.jolijob.profile.application.OwnProfileNotFoundException;
import com.example.jolijob.profile.application.port.in.CandidateProfileUseCase;
import com.example.jolijob.profile.application.port.in.CompanyProfileUseCase;
import com.example.jolijob.profile.domain.CandidateProfile;
import com.example.jolijob.profile.domain.CompanyProfile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/profiles")
public class InternalProfileController {
    private final CandidateProfileUseCase candidates;
    private final CompanyProfileUseCase companies;

    public InternalProfileController(CandidateProfileUseCase candidates, CompanyProfileUseCase companies) {
        this.candidates = candidates;
        this.companies = companies;
    }

    @GetMapping("/candidates/by-user/{userId}")
    public CandidateLookup candidateByUser(@PathVariable UUID userId) {
        try {
            CandidateProfile profile = candidates.getByUserId(userId);
            return new CandidateLookup(profile.id(), profile.details().cvUrl());
        } catch (OwnProfileNotFoundException exception) {
            throw new ProfileNotFoundException(exception.getMessage());
        }
    }

    @GetMapping("/companies/by-user/{userId}")
    public CompanyLookup companyByUser(@PathVariable UUID userId) {
        try {
            return CompanyLookup.from(companies.getByUserId(userId));
        } catch (OwnProfileNotFoundException exception) {
            throw new ProfileNotFoundException(exception.getMessage());
        }
    }

    @GetMapping("/companies/{companyId}")
    public CompanyLookup companyById(@PathVariable UUID companyId) {
        return CompanyLookup.from(companies.getById(companyId));
    }

    public record CandidateLookup(UUID id, String cvUrl) {
    }

    public record CompanyLookup(UUID id, String contactEmail) {
        static CompanyLookup from(CompanyProfile profile) {
            return new CompanyLookup(profile.id(), profile.details().contactEmail());
        }
    }
}

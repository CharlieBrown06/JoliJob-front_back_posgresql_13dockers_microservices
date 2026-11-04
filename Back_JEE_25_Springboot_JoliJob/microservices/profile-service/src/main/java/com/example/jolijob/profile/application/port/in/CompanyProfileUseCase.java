package com.example.jolijob.profile.application.port.in;

import com.example.jolijob.profile.domain.CompanyDetails;
import com.example.jolijob.profile.domain.CompanyProfile;

import java.util.UUID;

public interface CompanyProfileUseCase {
    CompanyProfile create(UUID userId, CompanyDetails details);
    CompanyProfile update(UUID userId, CompanyDetails details);
    CompanyProfile getByUserId(UUID userId);
    CompanyProfile getById(UUID companyId);
}

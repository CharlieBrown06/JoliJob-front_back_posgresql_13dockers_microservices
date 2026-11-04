package com.example.jolijob.application.adapter.out;

import com.example.jolijob.application.port.out.CompanyProfilePort;
import com.example.jolijob.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CompanyProfileJpaAdapter implements CompanyProfilePort {

    private final CompanyRepository companyRepository;

    @Override
    public Optional<CompanyProfile> findByUserId(UUID userId) {
        return companyRepository.findByUserId(userId)
                .map(company -> new CompanyProfile(company.getId(), company.getContactEmail()));
    }

    @Override
    public Optional<CompanyProfile> findById(UUID companyId) {
        return companyRepository.findById(companyId)
                .map(company -> new CompanyProfile(company.getId(), company.getContactEmail()));
    }
}

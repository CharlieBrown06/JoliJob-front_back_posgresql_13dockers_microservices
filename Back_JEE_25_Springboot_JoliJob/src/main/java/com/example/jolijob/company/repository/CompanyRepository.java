package com.example.jolijob.company.repository;

import com.example.jolijob.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Optional<Company> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}

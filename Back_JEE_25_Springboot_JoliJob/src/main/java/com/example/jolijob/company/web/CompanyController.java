package com.example.jolijob.company.web;

import com.example.jolijob.company.service.CompanyService;
import com.example.jolijob.company.web.dto.CompanyRequest;
import com.example.jolijob.company.web.dto.CompanyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Tag(name = "Entreprises", description = "Gestion des profils entreprises")
@SecurityRequirement(name = "bearerAuth")
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping("/profile")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "Créer le profil entreprise")
    public CompanyResponse createProfile(@Valid @RequestBody CompanyRequest request) {
        return companyService.createProfile(request);
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "Mettre à jour le profil entreprise")
    public CompanyResponse updateProfile(@Valid @RequestBody CompanyRequest request) {
        return companyService.updateProfile(request);
    }

    @GetMapping("/profile/me")
    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "Mon profil entreprise")
    public CompanyResponse getMyProfile() {
        return companyService.getMyProfile();
    }

    @GetMapping("/{companyId}")
    @Operation(summary = "Profil entreprise public")
    public CompanyResponse getById(@PathVariable UUID companyId) {
        return companyService.getById(companyId);
    }
}

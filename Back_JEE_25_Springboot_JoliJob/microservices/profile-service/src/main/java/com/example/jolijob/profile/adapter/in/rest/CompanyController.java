package com.example.jolijob.profile.adapter.in.rest;

import com.example.jolijob.profile.application.port.in.CompanyProfileUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {
    private final CompanyProfileUseCase profiles;

    public CompanyController(CompanyProfileUseCase profiles) {
        this.profiles = profiles;
    }

    @PostMapping("/profile")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COMPANY')")
    public CompanyResponse createProfile(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody CompanyRequest request) {
        return CompanyResponse.from(profiles.create(CurrentUser.id(jwt), request.toDetails()));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('COMPANY')")
    public CompanyResponse updateProfile(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody CompanyRequest request) {
        return CompanyResponse.from(profiles.update(CurrentUser.id(jwt), request.toDetails()));
    }

    @GetMapping("/profile/me")
    @PreAuthorize("hasRole('COMPANY')")
    public CompanyResponse getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return CompanyResponse.from(profiles.getByUserId(CurrentUser.id(jwt)));
    }

    @GetMapping("/{companyId}")
    public CompanyResponse getById(@PathVariable UUID companyId) {
        return CompanyResponse.from(profiles.getById(companyId));
    }
}

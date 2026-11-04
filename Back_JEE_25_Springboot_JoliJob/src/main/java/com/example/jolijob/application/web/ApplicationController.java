package com.example.jolijob.application.web;

import com.example.jolijob.application.service.ApplicationService;
import com.example.jolijob.application.web.dto.ApplicationRequest;
import com.example.jolijob.application.web.dto.ApplicationResponse;
import com.example.jolijob.application.web.dto.ApplicationStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Candidatures", description = "Dépôt et suivi des candidatures")
@SecurityRequirement(name = "bearerAuth")
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Postuler à une offre")
    public ApplicationResponse apply(@Valid @RequestBody ApplicationRequest request) {
        return applicationService.apply(request);
    }

    @PatchMapping("/{applicationId}/status")
    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "Mettre à jour le statut d'une candidature")
    public ApplicationResponse updateStatus(
            @PathVariable UUID applicationId,
            @Valid @RequestBody ApplicationStatusRequest request) {
        return applicationService.updateStatus(applicationId, request);
    }

    @PatchMapping("/{applicationId}/withdraw")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Retirer sa candidature")
    public ApplicationResponse withdraw(@PathVariable UUID applicationId) {
        return applicationService.withdraw(applicationId);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Mes candidatures")
    public List<ApplicationResponse> getMyCandidateApplications() {
        return applicationService.getMyCandidateApplications();
    }

    @GetMapping("/offer/{jobOfferId}")
    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "Candidatures reçues pour une offre")
    public List<ApplicationResponse> getOfferApplications(@PathVariable UUID jobOfferId) {
        return applicationService.getOfferApplications(jobOfferId);
    }
}

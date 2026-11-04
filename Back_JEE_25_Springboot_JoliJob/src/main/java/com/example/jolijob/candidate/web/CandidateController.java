package com.example.jolijob.candidate.web;

import com.example.jolijob.candidate.service.CandidateService;
import com.example.jolijob.candidate.web.dto.CandidateRequest;
import com.example.jolijob.candidate.web.dto.CandidateResponse;
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
@RequestMapping("/api/v1/candidates")
@RequiredArgsConstructor
@Tag(name = "Candidats", description = "Gestion des profils candidats")
@SecurityRequirement(name = "bearerAuth")
public class CandidateController {

    private final CandidateService candidateService;

    @PostMapping("/profile")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Créer le profil candidat")
    public CandidateResponse createProfile(@Valid @RequestBody CandidateRequest request) {
        return candidateService.createProfile(request);
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Mettre à jour le profil candidat")
    public CandidateResponse updateProfile(@Valid @RequestBody CandidateRequest request) {
        return candidateService.updateProfile(request);
    }

    @GetMapping("/profile/me")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Mon profil candidat")
    public CandidateResponse getMyProfile() {
        return candidateService.getMyProfile();
    }

    @GetMapping("/{candidateId}")
    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "Voir le profil d'un candidat (réservé entreprises)")
    public CandidateResponse getById(@PathVariable UUID candidateId) {
        return candidateService.getById(candidateId);
    }
}

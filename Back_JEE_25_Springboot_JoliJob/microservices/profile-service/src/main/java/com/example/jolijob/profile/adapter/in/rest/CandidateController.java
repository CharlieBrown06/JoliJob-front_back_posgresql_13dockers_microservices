package com.example.jolijob.profile.adapter.in.rest;

import com.example.jolijob.profile.application.port.in.CandidateProfileUseCase;
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
@RequestMapping("/api/v1/candidates")
public class CandidateController {
    private final CandidateProfileUseCase profiles;

    public CandidateController(CandidateProfileUseCase profiles) {
        this.profiles = profiles;
    }

    @PostMapping("/profile")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CANDIDATE')")
    public CandidateResponse createProfile(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody CandidateRequest request) {
        return CandidateResponse.from(profiles.create(CurrentUser.id(jwt), request.toDetails()));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('CANDIDATE')")
    public CandidateResponse updateProfile(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody CandidateRequest request) {
        return CandidateResponse.from(profiles.update(CurrentUser.id(jwt), request.toDetails()));
    }

    @GetMapping("/profile/me")
    @PreAuthorize("hasRole('CANDIDATE')")
    public CandidateResponse getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return CandidateResponse.from(profiles.getByUserId(CurrentUser.id(jwt)));
    }

    @GetMapping("/{candidateId}")
    @PreAuthorize("hasRole('COMPANY')")
    public CandidateResponse getById(@PathVariable UUID candidateId) {
        return CandidateResponse.from(profiles.getById(candidateId));
    }
}

package com.example.jolijob.auth.web;

import com.example.jolijob.auth.domain.UserRole;
import com.example.jolijob.auth.service.AuthService;
import com.example.jolijob.auth.web.dto.LoginRequest;
import com.example.jolijob.auth.web.dto.LoginResponse;
import com.example.jolijob.auth.web.dto.RegisterRequest;
import com.example.jolijob.auth.web.dto.RegisterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription et connexion candidats/entreprises")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/candidate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inscription candidat")
    public RegisterResponse registerCandidate(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request, UserRole.CANDIDATE);
    }

    @PostMapping("/register/company")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inscription entreprise")
    public RegisterResponse registerCompany(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request, UserRole.COMPANY);
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion — retourne un JWT Bearer")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}

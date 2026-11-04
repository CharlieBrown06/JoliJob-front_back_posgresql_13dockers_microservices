package com.example.jolijob.auth.service;

import com.example.jolijob.auth.domain.AppUser;
import com.example.jolijob.auth.domain.UserRole;
import com.example.jolijob.auth.repository.UserRepository;
import com.example.jolijob.auth.web.dto.LoginRequest;
import com.example.jolijob.auth.web.dto.LoginResponse;
import com.example.jolijob.auth.web.dto.RegisterRequest;
import com.example.jolijob.auth.web.dto.RegisterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    @Transactional
    public RegisterResponse register(RegisterRequest request, UserRole role) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("Email déjà utilisé : " + request.email());
        }
        AppUser user = new AppUser(
                request.email(),
                passwordEncoder.encode(request.password()),
                role
        );
        user = userRepository.save(user);
        return new RegisterResponse(user.getId().toString(), user.getEmail(), user.getRole().name());
    }

    public LoginResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Identifiants invalides");
        }
        String token = generateToken(user);
        return new LoginResponse(token, "Bearer", 3600L, user.getRole().name(), user.getId().toString());
    }

    private String generateToken(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("http://localhost:8080")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("userId", user.getId().toString())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}

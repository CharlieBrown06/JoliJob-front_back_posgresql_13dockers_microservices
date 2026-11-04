package com.example.jolijob.application.adapter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
                          @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
                          @Value("${spring.security.oauth2.resourceserver.jwt.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ofSeconds(60)),
                new JwtIssuerValidator(issuer),
                audienceValidator(audience),
                subjectValidator(),
                expirationValidator(),
                claimTypesValidator(),
                candidateEmailValidator()));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(roleClaimConverter())))
                .build();
    }

    private Converter<Jwt, AbstractAuthenticationToken> roleClaimConverter() {
        return jwt -> {
            List<SimpleGrantedAuthority> authorities = realmRoles(jwt).stream()
                    .filter(role -> role.equals("CANDIDATE") || role.equals("COMPANY"))
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
            return new JwtAuthenticationToken(jwt, authorities);
        };
    }

    private static OAuth2TokenValidator<Jwt> audienceValidator(String audience) {
        return jwt -> jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                "Required audience is missing", null));
    }

    private static OAuth2TokenValidator<Jwt> subjectValidator() {
        return jwt -> {
            try {
                UUID.fromString(jwt.getSubject());
                return OAuth2TokenValidatorResult.success();
            } catch (IllegalArgumentException | NullPointerException exception) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                        "JWT subject must be a UUID", null));
            }
        };
    }

    private static OAuth2TokenValidator<Jwt> expirationValidator() {
        return jwt -> jwt.getExpiresAt() != null
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "JWT expiration is required", null));
    }

    private static OAuth2TokenValidator<Jwt> claimTypesValidator() {
        return jwt -> {
            Object realmAccess = jwt.getClaims().get("realm_access");
            Object email = jwt.getClaims().get("email");
            Object emailVerified = jwt.getClaims().get("email_verified");
            boolean rolesValid = realmAccess == null || realmAccess instanceof Map<?, ?> claims
                    && (claims.get("roles") == null || claims.get("roles") instanceof List<?> roles
                    && roles.stream().allMatch(String.class::isInstance));
            boolean emailValid = email == null || email instanceof String;
            boolean emailVerifiedValid = emailVerified == null || emailVerified instanceof Boolean;
            return rolesValid && emailValid && emailVerifiedValid
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "JWT contains claims with invalid types", null));
        };
    }

    private static OAuth2TokenValidator<Jwt> candidateEmailValidator() {
        return jwt -> {
            Object emailClaim = jwt.getClaims().get("email");
            String email = emailClaim instanceof String value ? value : null;
            return realmRoles(jwt).contains("CANDIDATE")
                    && (!Boolean.TRUE.equals(jwt.getClaim("email_verified"))
                    || email == null || email.isBlank())
                    ? OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                    "Candidate tokens require a verified email address", null))
                    : OAuth2TokenValidatorResult.success();
        };
    }

    private static List<String> realmRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (!(realmAccess instanceof Map<?, ?> claims)
                || !(claims.get("roles") instanceof List<?> roles)) {
            return List.of();
        }
        return roles.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }
}

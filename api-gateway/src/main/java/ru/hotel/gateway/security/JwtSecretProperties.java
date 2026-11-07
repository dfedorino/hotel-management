package ru.hotel.gateway.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Shared HS256 secret, the same as in the services; at least 32 bytes for HMAC-SHA256. */
@Validated
@ConfigurationProperties("security.jwt")
public record JwtSecretProperties(@NotBlank @Size(min = 32) String secret) {}

package ru.hotel.hotelservice.security;

import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Shared HS256 secret; at least 32 bytes as required for HMAC-SHA256. */
@Validated
@ConfigurationProperties("security.jwt")
public record JwtProperties(@Size(min = 32) String secret) {}

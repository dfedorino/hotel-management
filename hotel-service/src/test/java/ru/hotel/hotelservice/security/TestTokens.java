package ru.hotel.hotelservice.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Issues HS256 tokens the way Booking Service does, for security tests.
 */
public final class TestTokens {

    public static final String SECRET = "test-secret-test-secret-test-secret-0123";

    private TestTokens() {
    }

    public static String token(String subject, String role) {
        return sign(SECRET, subject, role, Instant.now().plus(Duration.ofHours(1)));
    }

    public static String expiredToken(String role) {
        return sign(SECRET, "late", role, Instant.now().minus(Duration.ofMinutes(5)));
    }

    public static String foreignToken(String role) {
        return sign("another-secret-another-secret-another-1234", "intruder", role,
                Instant.now().plus(Duration.ofHours(1)));
    }

    private static String sign(String secret, String subject, String role, Instant expiresAt) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim("roles", List.of(role))
                .issueTime(Date.from(Instant.now().minus(Duration.ofHours(2))))
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (JOSEException ex) {
            throw new IllegalStateException(ex);
        }
        return jwt.serialize();
    }
}

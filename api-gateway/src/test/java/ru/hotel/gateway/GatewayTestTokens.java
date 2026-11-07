package ru.hotel.gateway;

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

/** HS256 tokens shaped like the ones Booking Service issues. */
final class GatewayTestTokens {

    static final String SECRET = "test-secret-test-secret-test-secret-0123";
    private static final String FOREIGN_SECRET = "another-secret-another-secret-another-1234";

    private GatewayTestTokens() {}

    static String valid() {
        return sign(SECRET, Instant.now().plus(Duration.ofHours(1)));
    }

    static String expired() {
        return sign(SECRET, Instant.now().minus(Duration.ofMinutes(5)));
    }

    static String signedWithForeignSecret() {
        return sign(FOREIGN_SECRET, Instant.now().plus(Duration.ofHours(1)));
    }

    private static String sign(String secret, Instant expiresAt) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("alice")
                .claim("roles", List.of("USER"))
                .issueTime(Date.from(expiresAt.minus(Duration.ofHours(1))))
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

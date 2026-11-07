package ru.hotel.bookingservice.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import ru.hotel.bookingapi.model.TokenResponse;
import ru.hotel.bookingservice.domain.Role;
import ru.hotel.bookingservice.domain.User;
import ru.hotel.security.jwt.JwtProperties;

@SuppressWarnings("PMD.HardCodedCryptoKey") // throwaway secret, stands in for the configured one
class JwtTokenIssuerTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-0123";
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    private final JwtTokenIssuer issuer = new JwtTokenIssuer(
            new JwtProperties(SECRET), new TokenProperties(Duration.ofHours(1)), Clock.fixed(NOW, ZoneOffset.UTC));

    private final NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(
                    new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();

    @Test
    void userTokenCarriesIdentityAndRoleAndLivesOneHour() {
        // given
        User user = new User("alice", "hash", Role.ADMIN);
        user.setId(7L);

        // when
        TokenResponse response = issuer.issue(user);

        // then
        Jwt jwt = decoder.decode(response.getAccessToken());
        assertThat(jwt.getSubject()).isEqualTo("alice");
        assertThat(jwt.getClaimAsString(JwtTokenIssuer.USER_ID_CLAIM)).isEqualTo("7");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
    }

    @Test
    void serviceTokenCarriesOnlyTheInternalRole() {
        // when
        ServiceToken token = issuer.issueServiceToken();

        // then
        Jwt jwt = decoder.decode(token.value());
        assertThat(jwt.getSubject()).isEqualTo(JwtTokenIssuer.SERVICE_SUBJECT);
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("INTERNAL");
        assertThat(token.expiresAt()).isEqualTo(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
    }
}

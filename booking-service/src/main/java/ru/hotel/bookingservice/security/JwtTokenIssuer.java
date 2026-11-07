package ru.hotel.bookingservice.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;
import ru.hotel.bookingapi.model.TokenResponse;
import ru.hotel.bookingservice.domain.User;
import ru.hotel.security.jwt.JwtProperties;

/** HS256 tokens: subject is the username, {@code uid} the user id, {@code roles} the role list. */
@Component
public class JwtTokenIssuer implements TokenIssuer {

    static final String USER_ID_CLAIM = "uid";
    static final String SERVICE_SUBJECT = "booking-service";
    private static final String ROLES_CLAIM = "roles";
    private static final String INTERNAL_ROLE = "INTERNAL";
    private static final Duration SERVICE_TOKEN_TTL = Duration.ofMinutes(5);

    private final JwtEncoder encoder;
    private final Duration userTokenTtl;
    private final Clock clock;

    public JwtTokenIssuer(JwtProperties jwtProperties, TokenProperties tokenProperties, Clock clock) {
        SecretKeySpec key = new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.userTokenTtl = tokenProperties.ttl();
        this.clock = clock;
    }

    @Override
    public TokenResponse issue(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getUsername())
                .claim(USER_ID_CLAIM, String.valueOf(user.getId()))
                .claim(ROLES_CLAIM, List.of(user.getRole().name()))
                .issuedAt(now)
                .expiresAt(now.plus(userTokenTtl))
                .build();
        return new TokenResponse(encode(claims), "Bearer", userTokenTtl.toSeconds());
    }

    @Override
    public ServiceToken issueServiceToken() {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(SERVICE_TOKEN_TTL);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(SERVICE_SUBJECT)
                .claim(ROLES_CLAIM, List.of(INTERNAL_ROLE))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        return new ServiceToken(encode(claims), expiresAt);
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}

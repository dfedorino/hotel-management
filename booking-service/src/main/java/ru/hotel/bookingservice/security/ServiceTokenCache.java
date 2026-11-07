package ru.hotel.bookingservice.security;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * Reuses the INTERNAL service token until {@link #REFRESH_MARGIN} before it expires, so a call never leaves with a
 * token about to run out. Lock-free: concurrent callers may both refresh once, which only costs one extra signature.
 */
@Component
public class ServiceTokenCache {

    static final Duration REFRESH_MARGIN = Duration.ofSeconds(30);

    private final TokenIssuer tokenIssuer;
    private final Clock clock;
    private final AtomicReference<ServiceToken> current = new AtomicReference<>();

    public ServiceTokenCache(TokenIssuer tokenIssuer, Clock clock) {
        this.tokenIssuer = tokenIssuer;
        this.clock = clock;
    }

    public String get() {
        ServiceToken token = current.get();
        if (token == null || isAboutToExpire(token)) {
            token = tokenIssuer.issueServiceToken();
            current.set(token);
        }
        return token.value();
    }

    private boolean isAboutToExpire(ServiceToken token) {
        return !clock.instant().isBefore(token.expiresAt().minus(REFRESH_MARGIN));
    }
}

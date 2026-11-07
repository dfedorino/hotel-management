package ru.hotel.bookingservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceTokenCacheTest {

    private static final Instant START = Instant.parse("2027-01-01T10:00:00Z");

    @Mock
    private TokenIssuer tokenIssuer;

    private final MovableClock clock = new MovableClock(START);

    private ServiceTokenCache cache;

    @BeforeEach
    void setUp() {
        cache = new ServiceTokenCache(tokenIssuer, clock);
    }

    @Test
    void reusesTokenWhileItIsFarFromExpiry() {
        // given
        given(tokenIssuer.issueServiceToken()).willReturn(token("first", START.plus(Duration.ofMinutes(5))));

        // when
        String first = cache.get();
        clock.advance(Duration.ofMinutes(4));
        String second = cache.get();

        // then
        assertThat(first).isEqualTo("first");
        assertThat(second).isEqualTo("first");
        verify(tokenIssuer, times(1)).issueServiceToken();
    }

    @Test
    void issuesNewTokenShortlyBeforeExpiry() {
        // given
        given(tokenIssuer.issueServiceToken())
                .willReturn(
                        token("first", START.plus(Duration.ofMinutes(5))),
                        token("second", START.plus(Duration.ofMinutes(10))));
        cache.get();

        // when
        clock.advance(Duration.ofMinutes(5).minus(ServiceTokenCache.REFRESH_MARGIN));
        String refreshed = cache.get();

        // then
        assertThat(refreshed).isEqualTo("second");
        verify(tokenIssuer, times(2)).issueServiceToken();
    }

    private static ServiceToken token(String value, Instant expiresAt) {
        return new ServiceToken(value, expiresAt);
    }

    /** Clock the test moves forward explicitly. */
    private static final class MovableClock extends Clock {

        private Instant now;

        MovableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}

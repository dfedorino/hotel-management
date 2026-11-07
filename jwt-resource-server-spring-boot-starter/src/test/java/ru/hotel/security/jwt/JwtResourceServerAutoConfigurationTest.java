package ru.hotel.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

class JwtResourceServerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(
                    AutoConfigurations.of(JacksonAutoConfiguration.class, JwtResourceServerAutoConfiguration.class))
            .withPropertyValues("security.jwt.secret=" + JwtTestTokens.SECRET);

    @Test
    void decodesTokenSignedWithSharedSecretAndMapsRolesToAuthorities() {
        contextRunner.run(context -> {
            // given
            JwtDecoder decoder = context.getBean(JwtDecoder.class);
            JwtAuthenticationConverter converter = context.getBean(JwtAuthenticationConverter.class);

            // when
            Jwt jwt = decoder.decode(
                    JwtTestTokens.sign(JwtTestTokens.SECRET, "alice", List.of("USER", "ADMIN"), inHour()));
            AbstractAuthenticationToken authentication = converter.convert(jwt);

            // then
            assertThat(authentication.getName()).isEqualTo("alice");
            assertThat(authentication.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        });
    }

    @Test
    void rejectsForeignSignatureAndExpiredToken() {
        contextRunner.run(context -> {
            // given
            JwtDecoder decoder = context.getBean(JwtDecoder.class);
            String foreign = JwtTestTokens.sign(JwtTestTokens.FOREIGN_SECRET, "eve", List.of("ADMIN"), inHour());
            String expired = JwtTestTokens.sign(
                    JwtTestTokens.SECRET, "late", List.of("USER"), Instant.now().minus(Duration.ofMinutes(5)));

            // when / then
            assertThatThrownBy(() -> decoder.decode(foreign)).isInstanceOf(JwtException.class);
            assertThatThrownBy(() -> decoder.decode(expired)).isInstanceOf(JwtException.class);
        });
    }

    @Test
    void refusesToStartWithShortSecret() {
        contextRunner
                .withPropertyValues("security.jwt.secret=too-short")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void providesJsonErrorWriterForSecurityFailures() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(SecurityErrorHandler.class));
    }

    @Test
    void providesFilterChainCustomizerAsAnInjectableBean() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(JwtResourceServerCustomizer.class));
    }

    private static Instant inHour() {
        return Instant.now().plus(Duration.ofHours(1));
    }
}

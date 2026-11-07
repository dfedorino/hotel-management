package ru.hotel.bookingservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import ru.hotel.security.jwt.JwtResourceServerConfigurer;

/** Registration and login are public; everything else needs a token, roles are checked by {@code @PreAuthorize}. */
@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_POSTS = {"/user/register", "/user/auth"};
    private static final String HEALTH_PATH = "/actuator/health";

    @Bean
    @SuppressWarnings("PMD.SignatureDeclareThrowsException") // HttpSecurity.build() declares Exception
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.with(new JwtResourceServerConfigurer(), configurer -> {})
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.POST, PUBLIC_POSTS)
                        .permitAll()
                        .requestMatchers(HEALTH_PATH)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

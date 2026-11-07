package ru.hotel.bookingservice.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Lifetime of user tokens; one hour by the requirements. */
@ConfigurationProperties("booking.token")
public record TokenProperties(@DefaultValue("PT1H") Duration ttl) {}

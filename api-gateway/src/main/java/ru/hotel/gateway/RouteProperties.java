package ru.hotel.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Backend locations; {@code lb://} resolves them through Eureka, tests point them at stubs. */
@ConfigurationProperties("gateway.routes")
public record RouteProperties(
        @DefaultValue("lb://booking-service") String bookingServiceUri,
        @DefaultValue("lb://hotel-service") String hotelServiceUri) {}

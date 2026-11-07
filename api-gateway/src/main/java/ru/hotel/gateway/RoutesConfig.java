package ru.hotel.gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Public routes. Booking Service paths are published under {@code /api/bookings} and reach the service without that
 * prefix ({@code /api/bookings/user/register} becomes {@code /user/register}). Rooms are routed one path segment deep
 * only, so the internal {@code /api/rooms/{id}/confirm-availability} and {@code /release} are never exposed.
 */
@Configuration
public class RoutesConfig {

    private static final int BOOKINGS_PREFIX_PARTS = 2;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder, RouteProperties properties) {
        return builder.routes()
                .route(
                        "booking-service",
                        route -> route.path("/api/bookings/**")
                                .filters(filters -> filters.stripPrefix(BOOKINGS_PREFIX_PARTS))
                                .uri(properties.bookingServiceUri()))
                .route(
                        "hotels",
                        route -> route.path("/api/hotels", "/api/hotels/**").uri(properties.hotelServiceUri()))
                .route(
                        "rooms",
                        route ->
                                route.path("/api/rooms", "/api/rooms/{segment}").uri(properties.hotelServiceUri()))
                .build();
    }
}

package ru.hotel.gateway;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Public routes; paths reach the services unchanged. Rooms are routed one path segment deep only, so the internal
 * {@code /api/rooms/{id}/confirm-availability} and {@code /release} are never exposed.
 */
@Configuration
public class RoutesConfig {

    private static final String LOAD_BALANCED = "lb";

    @Bean
    public RouterFunction<ServerResponse> bookingRoutes(RouteProperties properties) {
        return toBackend(
                        proxy("booking-service", path("/api/bookings", "/api/bookings/**")),
                        properties.bookingServiceUri())
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> hotelRoutes(RouteProperties properties) {
        return toBackend(proxy("hotels", path("/api/hotels", "/api/hotels/**")), properties.hotelServiceUri())
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> roomRoutes(RouteProperties properties) {
        return toBackend(proxy("rooms", path("/api/rooms", "/api/rooms/{segment}")), properties.hotelServiceUri())
                .build();
    }

    private static RouterFunctions.Builder proxy(String id, RequestPredicate predicate) {
        return route(id).route(predicate, http());
    }

    /** {@code lb://name} goes through Eureka and the load balancer; any other URI is called directly. */
    private static RouterFunctions.Builder toBackend(RouterFunctions.Builder builder, String location) {
        URI backend = URI.create(location);
        return LOAD_BALANCED.equals(backend.getScheme())
                ? builder.filter(lb(backend.getHost()))
                : builder.before(uri(backend));
    }
}

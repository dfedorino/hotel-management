package ru.hotel.bookingservice.client;

import feign.RequestInterceptor;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import ru.hotel.bookingservice.security.TokenIssuer;
import ru.hotel.hotelapi.api.RoomAvailabilityApiClient;
import ru.hotel.hotelapi.api.RoomsApiClient;

/** Generated Hotel Service clients, authenticated as Booking Service itself (role INTERNAL), not as the user. */
@Configuration
@EnableFeignClients(clients = {RoomAvailabilityApiClient.class, RoomsApiClient.class})
public class HotelClientConfig {

    @Bean
    public RequestInterceptor serviceTokenInterceptor(TokenIssuer tokenIssuer) {
        return template -> template.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenIssuer.issueServiceToken());
    }
}

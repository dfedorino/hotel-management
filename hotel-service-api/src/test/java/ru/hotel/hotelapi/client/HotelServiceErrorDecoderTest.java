package ru.hotel.hotelapi.client;

import static org.assertj.core.api.Assertions.assertThat;

import feign.FeignException;
import feign.Request;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.hotel.hotelapi.error.HotelServiceBadRequestException;
import ru.hotel.hotelapi.error.HotelServiceClientException;
import ru.hotel.hotelapi.error.HotelServiceConflictException;
import ru.hotel.hotelapi.error.HotelServiceNotFoundException;

class HotelServiceErrorDecoderTest {

    private static final String METHOD_KEY = "RoomAvailabilityApiClient#confirmAvailability(Long,AvailabilityRequest)";

    private final HotelServiceErrorDecoder decoder = new HotelServiceErrorDecoder();

    @Test
    void conflictBecomesTypedExceptionWithServerMessage() {
        // given
        Response response = response(409, errorBody(409, "Room 5 is not available from 2027-01-10 to 2027-01-15"));

        // when
        Exception exception = decoder.decode(METHOD_KEY, response);

        // then
        assertThat(exception)
                .isInstanceOf(HotelServiceConflictException.class)
                .hasMessage("Room 5 is not available from 2027-01-10 to 2027-01-15");
        assertThat(((HotelServiceClientException) exception).getError().getPath())
                .isEqualTo("/api/rooms/5/confirm-availability");
    }

    @Test
    void notFoundAndBadRequestBecomeTypedExceptions() {
        // when
        Exception notFound = decoder.decode(METHOD_KEY, response(404, errorBody(404, "Room 5 not found")));
        Exception badRequest = decoder.decode(METHOD_KEY, response(400, errorBody(400, "bad dates")));

        // then
        assertThat(notFound).isInstanceOf(HotelServiceNotFoundException.class);
        assertThat(badRequest).isInstanceOf(HotelServiceBadRequestException.class);
    }

    @Test
    void unreadableBodyFallsBackToStatusDescription() {
        // when
        Exception exception = decoder.decode(METHOD_KEY, response(409, "<html>oops</html>"));

        // then
        assertThat(exception).isInstanceOf(HotelServiceConflictException.class).hasMessageContaining("409");
        assertThat(((HotelServiceClientException) exception).getStatus()).isEqualTo(409);
    }

    @Test
    void serverErrorsStayFeignExceptionsSoCallersCanRetry() {
        // when
        Exception exception = decoder.decode(METHOD_KEY, response(503, errorBody(503, "down")));

        // then
        assertThat(exception).isInstanceOf(FeignException.class).isNotInstanceOf(HotelServiceClientException.class);
        assertThat(((FeignException) exception).status()).isEqualTo(503);
    }

    private static String errorBody(int status, String message) {
        return """
                {"timestamp": "2027-01-01T10:00:00+03:00", "status": %d, "error": "Error",
                 "message": "%s", "path": "/api/rooms/5/confirm-availability"}
                """.formatted(status, message);
    }

    private static Response response(int status, String body) {
        Request request = Request.create(
                Request.HttpMethod.POST,
                "http://hotel-service/api/rooms/5/confirm-availability",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null);
        return Response.builder()
                .status(status)
                .reason("reason")
                .request(request)
                .headers(Map.of())
                .body(body, StandardCharsets.UTF_8)
                .build();
    }
}

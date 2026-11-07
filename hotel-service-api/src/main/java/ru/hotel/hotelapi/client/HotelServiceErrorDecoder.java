package ru.hotel.hotelapi.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import ru.hotel.hotelapi.error.HotelServiceBadRequestException;
import ru.hotel.hotelapi.error.HotelServiceConflictException;
import ru.hotel.hotelapi.error.HotelServiceNotFoundException;
import ru.hotel.hotelapi.model.ErrorResponse;

/**
 * Maps rejected requests (400, 404, 409) to typed exceptions; anything else, including 5xx, is left to Feign's default
 * decoder so that callers can treat it as a transient failure.
 */
@Slf4j
public class HotelServiceErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder fallback = new ErrorDecoder.Default();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Override
    public Exception decode(String methodKey, Response response) {
        return switch (response.status()) {
            case 400 -> new HotelServiceBadRequestException(readError(response));
            case 404 -> new HotelServiceNotFoundException(readError(response));
            case 409 -> new HotelServiceConflictException(readError(response));
            default -> fallback.decode(methodKey, response);
        };
    }

    private ErrorResponse readError(Response response) {
        if (response.body() != null) {
            try (InputStream body = response.body().asInputStream()) {
                return objectMapper.readValue(body, ErrorResponse.class);
            } catch (IOException ex) {
                log.debug("Hotel Service answered {} with a body that is not an ErrorResponse", response.status(), ex);
            }
        }
        return new ErrorResponse(
                OffsetDateTime.now(),
                response.status(),
                response.reason(),
                "Hotel Service responded with status " + response.status(),
                response.request().url());
    }
}

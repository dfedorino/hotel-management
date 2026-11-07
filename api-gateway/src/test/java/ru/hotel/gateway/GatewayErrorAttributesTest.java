package ru.hotel.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.RequestDispatcher;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

class GatewayErrorAttributesTest {

    private final GatewayErrorAttributes errorAttributes = new GatewayErrorAttributes();

    @Test
    void otherStatusesAreDescribedByTheirReasonPhrase() {
        // when
        Map<String, Object> body = attributes(500, null);

        // then
        assertThat(body)
                .containsEntry("status", 500)
                .containsEntry("message", "Internal Server Error")
                .containsEntry("path", "/api/hotels");
    }

    @Test
    void statusExceptionWithoutReasonFallsBackToReasonPhrase() {
        // when
        Map<String, Object> body = attributes(502, new ResponseStatusException(HttpStatus.BAD_GATEWAY));

        // then
        assertThat(body).containsEntry("message", "Bad Gateway");
    }

    @Test
    void unknownStatusGetsGenericMessage() {
        // when
        Map<String, Object> body = attributes(599, null);

        // then
        assertThat(body).containsEntry("status", 599).containsEntry("message", "Gateway error");
    }

    private Map<String, Object> attributes(int status, Exception error) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/hotels");
        if (error != null) {
            request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, error);
        }
        return errorAttributes.getErrorAttributes(new ServletWebRequest(request), ErrorAttributeOptions.defaults());
    }
}

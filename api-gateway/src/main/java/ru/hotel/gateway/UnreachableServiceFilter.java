package ru.hotel.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/** A service that cannot be reached (refused connection, timeout) is reported as 503 instead of a bare 500. */
@Slf4j
class UnreachableServiceFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    static final String MESSAGE = "Service is unavailable, try again later";

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        try {
            return next.handle(request);
        } catch (ResourceAccessException unreachable) {
            log.warn(
                    "{} {} could not reach the service: {}",
                    request.method(),
                    request.path(),
                    unreachable.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, MESSAGE, unreachable);
        }
    }
}

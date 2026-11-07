package ru.hotel.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** 401 in the same JSON shape as the services' ErrorResponse. */
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", HttpStatus.UNAUTHORIZED.getReasonPhrase());
        body.put("message", "Authentication required");
        body.put("path", exchange.getRequest().getPath().value());
        try {
            DataBuffer buffer = response.bufferFactory().wrap(objectMapper.writeValueAsBytes(body));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException serializationFailure) {
            return Mono.error(serializationFailure);
        }
    }
}

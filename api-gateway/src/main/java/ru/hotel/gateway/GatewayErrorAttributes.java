package ru.hotel.gateway;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

/**
 * Errors raised by the gateway itself (no route, service down) in the services' ErrorResponse shape: timestamp, status,
 * error, message, path. Exception details stay in the logs.
 */
@Component
public class GatewayErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> defaults = super.getErrorAttributes(webRequest, ErrorAttributeOptions.defaults());
        int status = (Integer) defaults.getOrDefault("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        String path = String.valueOf(defaults.get("path"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        body.put("status", status);
        body.put("error", defaults.get("error"));
        body.put("message", message(webRequest, status, path));
        body.put("path", path);
        return body;
    }

    private String message(WebRequest webRequest, int status, String path) {
        if (getError(webRequest) instanceof ResponseStatusException statusException
                && statusException.getReason() != null) {
            return statusException.getReason();
        }
        if (status == HttpStatus.NOT_FOUND.value()) {
            return "No route for " + path;
        }
        HttpStatus known = HttpStatus.resolve(status);
        return known == null ? "Gateway error" : known.getReasonPhrase();
    }
}

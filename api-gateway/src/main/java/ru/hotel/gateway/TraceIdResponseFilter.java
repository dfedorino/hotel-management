package ru.hotel.gateway;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.handler.TracingObservationHandler.TracingContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Returns the trace id of every request, including those rejected by security, so a client can quote it and the
 * matching log lines of the gateway and all services can be found by it. The trace is started by the observed
 * HttpHandler around all filters; this one runs before the security chain.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class TraceIdResponseFilter implements WebFilter {

    static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        exchange.getResponse().beforeCommit(() -> {
            ServerRequestObservationContext.findCurrent(exchange.getAttributes())
                    .map(TraceIdResponseFilter::traceId)
                    .ifPresent(traceId -> exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId));
            return Mono.empty();
        });
        return chain.filter(exchange);
    }

    // The tracing context belongs to Micrometer and lives as long as the request; closing it here would end the span.
    @SuppressWarnings("PMD.CloseResource")
    private static String traceId(ServerRequestObservationContext context) {
        TracingContext tracing = context.get(TracingContext.class);
        Span span = tracing == null ? null : tracing.getSpan();
        return span == null ? null : span.context().traceId();
    }
}

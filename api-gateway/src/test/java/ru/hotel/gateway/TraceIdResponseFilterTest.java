package ru.hotel.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class TraceIdResponseFilterTest {

    private final TraceIdResponseFilter filter = new TraceIdResponseFilter();

    @Test
    void addsNoHeaderWhenRequestIsNotTraced() {
        // given
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/hotels"));
        ServerRequestObservationContext untraced = new ServerRequestObservationContext(
                exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());
        exchange.getAttributes().put(ServerRequestObservationContext.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE, untraced);

        // when
        filter.filter(exchange, ignored -> Mono.empty())
                .then(exchange.getResponse().setComplete())
                .block();

        // then
        assertThat(exchange.getResponse().getHeaders()).doesNotContainKey(TraceIdResponseFilter.TRACE_ID_HEADER);
    }
}

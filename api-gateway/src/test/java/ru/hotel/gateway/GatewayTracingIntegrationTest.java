package ru.hotel.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
class GatewayTracingIntegrationTest {

    private static final String TRACE_ID = "[0-9a-f]{32}";

    @RegisterExtension
    static final WireMockExtension BOOKING = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void backends(DynamicPropertyRegistry registry) {
        registry.add("gateway.routes.booking-service-uri", BOOKING::baseUrl);
        registry.add("security.jwt.secret", () -> GatewayTestTokens.SECRET);
    }

    @Test
    void responseCarriesTraceIdThatIsPropagatedDownstream() throws Exception {
        // given
        BOOKING.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(201)));

        // when
        String traceId = mockMvc.perform(MockMvcRequestBuilders.post("/api/bookings/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader(TraceIdResponseFilter.TRACE_ID_HEADER);

        // then
        assertThat(traceId).matches(TRACE_ID);
        String traceparent = BOOKING.getAllServeEvents().getFirst().getRequest().getHeader("traceparent");
        assertThat(traceparent).contains(traceId);
    }

    @Test
    void rejectedRequestAlsoCarriesTraceId() throws Exception {
        // when / then
        mockMvc.perform(get("/api/hotels"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                                TraceIdResponseFilter.TRACE_ID_HEADER, org.hamcrest.Matchers.matchesRegex(TRACE_ID)));
    }
}

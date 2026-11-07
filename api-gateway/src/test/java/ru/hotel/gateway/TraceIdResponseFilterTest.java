package ru.hotel.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdResponseFilterTest {

    @Test
    void addsNoHeaderWhenRequestIsNotTraced() throws Exception {
        // given
        Tracer tracer = mock(Tracer.class);
        given(tracer.currentSpan()).willReturn(null);
        TraceIdResponseFilter filter = new TraceIdResponseFilter(tracer);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        // then
        assertThat(response.getHeader(TraceIdResponseFilter.TRACE_ID_HEADER)).isNull();
    }
}

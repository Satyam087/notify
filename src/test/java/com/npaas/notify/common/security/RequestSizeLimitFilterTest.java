package com.npaas.notify.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

class RequestSizeLimitFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final RequestSizeLimitFilter filter = new RequestSizeLimitFilter(
        8,
        objectMapper
    );

    @Test
    void rejectsNonPositiveConfiguredLimit() {
        assertThatThrownBy(() -> new RequestSizeLimitFilter(0, objectMapper))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("notify.security.max-event-body-bytes");
    }

    @Test
    void rejectsUnsafeConfiguredLimit() {
        assertThatThrownBy(() -> new RequestSizeLimitFilter(1_048_577, objectMapper))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("notify.security.max-event-body-bytes");
    }

    @Test
    void filtersEventsEndpointUsingServletPathEvenWithContextPath() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/notify/api/v1/events");
        request.setContextPath("/notify");
        request.setServletPath("/api/v1/events");
        request.setContent("0123456789".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(413);
    }

    @Test
    void filtersEventsEndpointWithTrailingSlash() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/events/");
        request.setServletPath("/api/v1/events/");
        request.setContent("0123456789".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(413);
    }

    @Test
    void filtersEventsEndpointWithMatrixParameters() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/events;source=test");
        request.setServletPath("/api/v1/events;source=test");
        request.setContent("0123456789".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(413);
    }

    @Test
    void skipsNonEventsEndpoints() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/templates");
        request.setServletPath("/api/v1/templates");
        request.setContent("0123456789".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsImmediatelyWhenContentLengthExceedsLimit() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/events") {
            @Override
            public long getContentLengthLong() {
                return 100;
            }
        };
        request.setServletPath("/api/v1/events");
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("Payload Too Large");
    }

    @Test
    void stillRejectsChunkedStyleBodiesWithoutContentLength() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/events");
        request.setServletPath("/api/v1/events");
        request.setContent("0123456789".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(413);
    }

    @Test
    void cachedRequestUsesUtf8WhenCharsetIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/events");
        request.setServletPath("/api/v1/events");
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> assertThatCode(
            () -> servletRequest.getReader().readLine()
        ).doesNotThrowAnyException();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void cachedRequestRejectsInvalidCharset() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/events");
        request.setServletPath("/api/v1/events");
        request.setContentType("application/json; charset=invalid-charset");
        request.setCharacterEncoding("invalid-charset");
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> assertThatThrownBy(
            servletRequest::getReader
        ).isInstanceOf(InvalidRequestCharsetException.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }
}

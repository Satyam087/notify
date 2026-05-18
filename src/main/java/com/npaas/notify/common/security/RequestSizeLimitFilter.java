package com.npaas.notify.common.security;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.npaas.notify.common.web.ApiErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private static final String EVENTS_PATH = "/api/v1/events";
    private static final long MAX_CONFIGURABLE_EVENT_BODY_BYTES = 1_048_576;

    private final long maxEventBodyBytes;
    private final ObjectMapper objectMapper;

    public RequestSizeLimitFilter(
            @Value("${notify.security.max-event-body-bytes:65536}") long maxEventBodyBytes,
            ObjectMapper objectMapper) {
        if (maxEventBodyBytes <= 0 || maxEventBodyBytes > MAX_CONFIGURABLE_EVENT_BODY_BYTES) {
            throw new IllegalArgumentException(
                "notify.security.max-event-body-bytes must be between 1 and "
                    + MAX_CONFIGURABLE_EVENT_BODY_BYTES);
        }
        this.maxEventBodyBytes = maxEventBodyBytes;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isEventsPath(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        BodyReadResult bodyReadResult = readBodyWithinLimit(request);
        if (bodyReadResult.tooLarge()) {
            writeError(response, request, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload Too Large",
                "Request body is too large");
            return;
        }

        filterChain.doFilter(new CachedBodyRequest(request, bodyReadResult.body()), response);
    }

    private BodyReadResult readBodyWithinLimit(HttpServletRequest request) throws IOException {
        if (request.getContentLengthLong() > maxEventBodyBytes) {
            return new BodyReadResult(new byte[0], true);
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        long total = 0;

        while ((read = request.getInputStream().read(buffer)) != -1) {
            total += read;
            if (total > maxEventBodyBytes) {
                int bytesWithinLimit = Math.max(0, read - (int) (total - maxEventBodyBytes));
                output.write(buffer, 0, bytesWithinLimit);
                return new BodyReadResult(output.toByteArray(), true);
            }

            output.write(buffer, 0, read);
        }

        return new BodyReadResult(output.toByteArray(), false);
    }

    private boolean isEventsPath(String servletPath) {
        if (servletPath == null) {
            return false;
        }

        String path = stripMatrixParameters(servletPath);
        return EVENTS_PATH.equals(path) || (EVENTS_PATH + "/").equals(path);
    }

    private String stripMatrixParameters(String path) {
        String[] segments = path.split("/", -1);
        for (int index = 0; index < segments.length; index++) {
            String segment = segments[index];
            int matrixStart = segment.indexOf(';');
            if (matrixStart >= 0) {
                segments[index] = segment.substring(0, matrixStart);
            }
        }
        return String.join("/", segments);
    }

    private void writeError(HttpServletResponse response, HttpServletRequest request, int status, String error,
            String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiErrorResponse.of(status, error, message,
            request.getRequestURI()));
    }

    private record BodyReadResult(byte[] body, boolean tooLarge) {
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException("Async request body reads are not supported");
                }

                @Override
                public int read() {
                    return input.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            Charset charset = resolveCharset();
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }

        private Charset resolveCharset() {
            String encoding = getCharacterEncoding();
            if (encoding == null || encoding.isBlank()) {
                return StandardCharsets.UTF_8;
            }

            try {
                return Charset.forName(encoding);
            } catch (IllegalCharsetNameException | UnsupportedCharsetException exception) {
                throw new InvalidRequestCharsetException(encoding, exception);
            }
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}

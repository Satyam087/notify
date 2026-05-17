package com.npaas.notify.common.security;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final long maxEventBodyBytes;

    public RequestSizeLimitFilter(@Value("${notify.security.max-event-body-bytes:65536}") long maxEventBodyBytes) {
        this.maxEventBodyBytes = maxEventBodyBytes;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().equals("/api/v1/events");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        BodyReadResult bodyReadResult = readBodyWithinLimit(request);
        if (bodyReadResult.tooLarge()) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            response.setContentType("application/json");
            response.getWriter().write("""
                {"timestamp":"%s","status":413,"error":"Payload Too Large","message":"Request body is too large"}
                """.formatted(Instant.now()));
            return;
        }

        filterChain.doFilter(new CachedBodyRequest(request, bodyReadResult.body()), response);
    }

    private BodyReadResult readBodyWithinLimit(HttpServletRequest request) throws IOException {
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
            Charset charset = getCharacterEncoding() == null
                    ? StandardCharsets.UTF_8
                    : Charset.forName(getCharacterEncoding());
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
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

package com.wayfarer.travel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class RequestGuard extends OncePerRequestFilter {
    private static final Set<String> MUTATIONS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final int MAX_BODY_BYTES = 16_384;
    private final ObjectMapper json;

    RequestGuard(ObjectMapper json) {
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        response.setHeader("Cache-Control", "no-store");
        if (!MUTATIONS.contains(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        // A cross-origin page cannot add this header without a successful CORS preflight.
        if (!"web".equals(request.getHeader("X-Travel-Client"))) {
            reject(response, 403, "INVALID_CLIENT", "请求来源验证失败，请刷新页面。");
            return;
        }
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            tooLarge(response);
            return;
        }
        // Tomcat enforces a known Content-Length. Cap streamed requests separately.
        if (request.getContentLengthLong() < 0) {
            byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
            if (body.length > MAX_BODY_BYTES) {
                tooLarge(response);
                return;
            }
            request = new BufferedRequest(request, body);
        }
        chain.doFilter(request, response);
    }

    private void tooLarge(HttpServletResponse response) throws IOException {
        reject(response, 413, "REQUEST_TOO_LARGE", "请求内容超过 16 KB。");
    }

    private void reject(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        json.writeValue(response.getOutputStream(), new ApiErrors.ErrorBody(code, message, false));
    }

    private static final class BufferedRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        BufferedRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override public int getContentLength() { return body.length; }
        @Override public long getContentLengthLong() { return body.length; }
        @Override public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
        @Override public ServletInputStream getInputStream() {
            var reader = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return reader.read(); }
                @Override public int read(byte[] value, int offset, int size) {
                    return reader.read(value, offset, size);
                }
                @Override public boolean isFinished() { return reader.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {
                    throw new IllegalStateException("Request body is read synchronously");
                }
            };
        }
    }
}

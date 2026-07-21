package com.clientdesk.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JsonRequestSizeLimitFilter extends OncePerRequestFilter {

    private final long maxRequestBytes;

    public JsonRequestSizeLimitFilter(
            @Value("${clientdesk.api.max-json-request-bytes:262144}") long maxRequestBytes
    ) {
        this.maxRequestBytes = maxRequestBytes;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isJson(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(new BoundedRequest(request, maxRequestBytes), response);
    }

    private boolean isJson(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return false;
        }
        try {
            return MediaType.APPLICATION_JSON.includes(MediaType.parseMediaType(contentType));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static final class BoundedRequest extends HttpServletRequestWrapper {

        private final long maxRequestBytes;

        private BoundedRequest(HttpServletRequest request, long maxRequestBytes) {
            super(request);
            this.maxRequestBytes = maxRequestBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new BoundedInputStream(super.getInputStream(), maxRequestBytes);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }

    private static final class BoundedInputStream extends ServletInputStream {

        private final ServletInputStream delegate;
        private final long maxRequestBytes;
        private long bytesRead;

        private BoundedInputStream(ServletInputStream delegate, long maxRequestBytes) {
            this.delegate = delegate;
            this.maxRequestBytes = maxRequestBytes;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value != -1) {
                recordBytes(1);
            }
            return value;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            int count = delegate.read(bytes, offset, length);
            if (count > 0) {
                recordBytes(count);
            }
            return count;
        }

        private void recordBytes(int count) throws RequestBodyTooLargeException {
            bytesRead += count;
            if (bytesRead > maxRequestBytes) {
                throw new RequestBodyTooLargeException();
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }
    }
}

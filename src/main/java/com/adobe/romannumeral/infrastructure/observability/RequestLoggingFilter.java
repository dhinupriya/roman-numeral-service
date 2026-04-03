package com.adobe.romannumeral.infrastructure.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Servlet filter that logs structured request/response information.
 *
 * <p>Logs after the request completes with: HTTP method, URI, query string,
 * response status, duration, client IP, and response size. The correlation ID is
 * automatically included via MDC (set by {@link CorrelationIdFilter}).
 *
 * <p>Additional MDC fields for structured JSON logging (searchable in Loki):
 * <ul>
 *   <li>{@code clientIp} — client IP address (supports X-Forwarded-For)</li>
 *   <li>{@code method} — HTTP method (GET, POST, etc.)</li>
 *   <li>{@code uri} — request URI with query string</li>
 *   <li>{@code status} — HTTP response status code</li>
 *   <li>{@code durationMs} — request duration in milliseconds</li>
 *   <li>{@code responseSize} — response body size in bytes</li>
 * </ul>
 *
 * <p>Runs after CorrelationIdFilter ({@code @Order(Ordered.HIGHEST_PRECEDENCE + 1)})
 * so the correlation ID is available in MDC when this filter logs.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.nanoTime();
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(request, wrappedResponse);
        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            String queryString = request.getQueryString();
            String uri = queryString != null
                    ? request.getRequestURI() + "?" + queryString
                    : request.getRequestURI();
            String clientIp = resolveClientIp(request);
            int responseSize = wrappedResponse.getContentSize();

            // Add structured fields to MDC — visible in JSON logs (Loki)
            MDC.put("clientIp", clientIp);
            MDC.put("method", request.getMethod());
            MDC.put("uri", uri);
            MDC.put("status", String.valueOf(wrappedResponse.getStatus()));
            MDC.put("durationMs", String.valueOf(durationMs));
            MDC.put("responseSize", String.valueOf(responseSize));

            log.info("{} {} → {} ({}ms, {}B) [{}]",
                    request.getMethod(),
                    uri,
                    wrappedResponse.getStatus(),
                    durationMs,
                    responseSize,
                    clientIp);

            // Clean up MDC fields
            MDC.remove("clientIp");
            MDC.remove("method");
            MDC.remove("uri");
            MDC.remove("status");
            MDC.remove("durationMs");
            MDC.remove("responseSize");

            // Copy response body back to the original response
            wrappedResponse.copyBodyToResponse();
        }
    }

    /**
     * Resolves the client IP address, respecting X-Forwarded-For header
     * from load balancers and reverse proxies.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For can contain multiple IPs: client, proxy1, proxy2
            // The first one is the original client
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Skip logging for actuator endpoints — they're called frequently by
     * Prometheus scraping and health checks, would flood the logs.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }
}

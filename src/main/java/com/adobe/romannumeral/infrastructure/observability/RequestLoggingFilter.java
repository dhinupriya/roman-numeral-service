package com.adobe.romannumeral.infrastructure.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that logs structured request/response information.
 *
 * <p>Logs after the request completes with: HTTP method, URI, query string,
 * response status, and duration in milliseconds. The correlation ID is
 * automatically included via MDC (set by {@link CorrelationIdFilter}).
 *
 * <p>Runs after CorrelationIdFilter ({@code @Order(Ordered.HIGHEST_PRECEDENCE + 1)})
 * so the correlation ID is available in MDC when this filter logs.
 *
 * <p>In the Docker profile, logback serializes these log entries as structured JSON
 * via logstash-logback-encoder — searchable and filterable in Loki/Grafana.
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

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            String queryString = request.getQueryString();
            String uri = queryString != null
                    ? request.getRequestURI() + "?" + queryString
                    : request.getRequestURI();

            log.info("{} {} → {} ({}ms)",
                    request.getMethod(),
                    uri,
                    response.getStatus(),
                    durationMs);
        }
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

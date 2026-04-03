package com.adobe.romannumeral.infrastructure.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that generates a unique correlation ID for every request.
 *
 * <p>The correlation ID is:
 * <ul>
 *   <li>Injected into <b>MDC</b> — appears in every log line for this request
 *       (visible in Loki via Grafana Explore)</li>
 *   <li>Added as <b>response header</b> ({@code X-Correlation-Id}) — callers can
 *       reference it in bug reports or support tickets</li>
 *   <li>Accepted from <b>incoming request header</b> — if the caller sends
 *       {@code X-Correlation-Id}, we propagate it instead of generating a new one.
 *       This enables distributed tracing across multiple services.</li>
 * </ul>
 *
 * <p>Runs first ({@code @Order(Ordered.HIGHEST_PRECEDENCE)}) so all subsequent
 * filters and the controller see the correlation ID in MDC.
 *
 * <p><b>Cleanup:</b> MDC is cleared in the {@code finally} block to prevent
 * thread-local leaks — critical when using virtual threads (they may be reused).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = extractOrGenerate(request);

        try {
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    /**
     * Uses the caller's correlation ID if provided, otherwise generates a new UUID.
     * Propagating the caller's ID enables distributed tracing across services.
     */
    private String extractOrGenerate(HttpServletRequest request) {
        String existing = request.getHeader(CORRELATION_ID_HEADER);
        return (existing != null && !existing.isBlank()) ? existing : UUID.randomUUID().toString();
    }
}

package com.adobe.romannumeral.infrastructure.security;

import com.adobe.romannumeral.infrastructure.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * API key authentication filter — validates the {@code X-API-Key} header.
 *
 * <p>Checks every request for a valid API key. Returns 401 Unauthorized if:
 * <ul>
 *   <li>The {@code X-API-Key} header is missing</li>
 *   <li>The key doesn't match any configured key in {@code app.security.api-keys}</li>
 * </ul>
 *
 * <p><b>Skips:</b> actuator endpoints ({@code /actuator/**}) and Swagger UI
 * ({@code /swagger-ui/**, /v3/api-docs/**}). These are infrastructure/documentation
 * endpoints that need to be accessible without API keys — used by load balancers,
 * Prometheus, and developers.
 *
 * <p><b>Filter ordering:</b> Runs after CorrelationIdFilter and RequestLoggingFilter
 * ({@code HIGHEST_PRECEDENCE + 2}) so that:
 * <ul>
 *   <li>401 responses have a correlation ID for debugging</li>
 *   <li>Rejected requests are logged (visible in Grafana)</li>
 *   <li>Auth happens before rate limiting — unauthenticated requests don't consume rate limit tokens</li>
 * </ul>
 *
 * <p><b>Production note:</b> API key auth is Tier 2 security — appropriate for
 * service-to-service communication. For multi-tenant production environments,
 * OAuth2/JWT at an API Gateway (Kong, AWS API Gateway) is recommended (Tier 3).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";

    private final AppProperties appProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Missing API key for {} {}", request.getMethod(), request.getRequestURI());
            FilterResponseHelper.writeErrorResponse(response, HttpStatus.UNAUTHORIZED,
                    "Missing API key. Provide a valid key via the X-API-Key header.");
            return;
        }

        if (!appProperties.getApiKeys().contains(apiKey.trim())) {
            log.warn("Invalid API key for {} {}", request.getMethod(), request.getRequestURI());
            FilterResponseHelper.writeErrorResponse(response, HttpStatus.UNAUTHORIZED,
                    "Invalid API key.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Skip authentication for actuator, Swagger UI, and API docs endpoints.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}

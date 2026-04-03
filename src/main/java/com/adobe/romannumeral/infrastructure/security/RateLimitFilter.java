package com.adobe.romannumeral.infrastructure.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Rate limiting filter using Bucket4j token-bucket algorithm.
 *
 * <p>Two separate buckets for different endpoint costs:
 * <ul>
 *   <li><b>Single queries</b> ({@code ?query=...}): 100 requests/second — cheap, O(1) cache lookup</li>
 *   <li><b>Range queries</b> ({@code ?min=...&max=...}): 10 requests/second — expensive, parallel computation</li>
 * </ul>
 *
 * <p><b>Why per-endpoint, not global?</b> Range queries are ~100x more expensive than
 * single queries (parallel computation vs. cache lookup). A global bucket would allow
 * a flood of range queries to consume the entire budget, starving single queries.
 *
 * <p><b>Why not per-client (IP-based)?</b> Proxies and load balancers complicate client
 * identification (X-Forwarded-For spoofing). Per-endpoint is sufficient for this service
 * and avoids that complexity.
 *
 * <p><b>Filter ordering:</b> Runs after ApiKeyAuthFilter ({@code HIGHEST_PRECEDENCE + 3})
 * so unauthenticated requests don't consume rate limit tokens.
 *
 * <p>Returns 429 Too Many Requests with a structured JSON error and {@code Retry-After} header.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
public class RateLimitFilter extends OncePerRequestFilter {

    private final Bucket singleQueryBucket;
    private final Bucket rangeQueryBucket;
    private final Counter rateLimitCounter;

    public RateLimitFilter(MeterRegistry registry) {
        this.rateLimitCounter = Counter.builder("roman_rate_limit_rejected_total")
                .description("Total number of requests rejected by rate limiting (429)")
                .register(registry);
        // Greedy refill: tokens are added continuously throughout the second,
        // providing smooth traffic shaping. This is production-grade behavior —
        // prevents bursty rejection patterns that interval refill would cause.
        //
        // Note: greedy refill cannot be exhausted by sequential curl requests
        // (tokens refill faster than curl overhead). Rate limiting is verified via:
        // 1. MockMvc unit tests (fast enough to outpace refill)
        // 2. k6 load tests with concurrent virtual users (Phase 6)
        this.singleQueryBucket = Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(100)
                        .refillGreedy(100, Duration.ofSeconds(1))
                        .build())
                .build();

        this.rangeQueryBucket = Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(10)
                        .refillGreedy(10, Duration.ofSeconds(1))
                        .build())
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Bucket bucket = selectBucket(request);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            rateLimitCounter.increment();
            log.warn("Rate limit exceeded for {} {}", request.getMethod(), request.getRequestURI());
            response.setHeader("Retry-After", "1");
            FilterResponseHelper.writeErrorResponse(response, HttpStatus.TOO_MANY_REQUESTS,
                    "Rate limit exceeded. Try again in 1 second.");
        }
    }

    /**
     * Selects the appropriate bucket based on query parameters.
     * Range queries (min + max params) use the stricter bucket.
     */
    private Bucket selectBucket(HttpServletRequest request) {
        String min = request.getParameter("min");
        String max = request.getParameter("max");
        if (min != null && max != null) {
            return rangeQueryBucket;
        }
        return singleQueryBucket;
    }

    /**
     * Skip rate limiting for actuator and documentation endpoints.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}

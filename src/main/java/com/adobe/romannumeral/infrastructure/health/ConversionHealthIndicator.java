package com.adobe.romannumeral.infrastructure.health;

import com.adobe.romannumeral.infrastructure.converter.CachedRomanNumeralConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator that verifies the core conversion logic is working.
 *
 * <p>The default Spring health check only confirms the JVM is alive.
 * This indicator runs a smoke test — {@code convert(1)} must return {@code "I"}.
 * If the conversion engine is broken (e.g., corrupted cache, algorithm bug),
 * {@code /actuator/health} returns DOWN and the load balancer stops routing
 * traffic to this instance.
 *
 * <p>Uses the cached converter (injected by Spring from ConverterConfig) —
 * the same converter that handles single queries. If the cache is healthy,
 * single queries work.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversionHealthIndicator implements HealthIndicator {

    private final CachedRomanNumeralConverter converter;

    @Override
    public Health health() {
        try {
            var result = converter.convert(1);
            if ("I".equals(result.output())) {
                return Health.up()
                        .withDetail("conversion", "convert(1) = I")
                        .build();
            }
            log.error("Health check failed: convert(1) returned '{}' instead of 'I'", result.output());
            return Health.down()
                    .withDetail("conversion", "convert(1) returned " + result.output())
                    .build();
        } catch (Exception e) {
            log.error("Health check failed with exception", e);
            return Health.down()
                    .withException(e)
                    .build();
        }
    }
}

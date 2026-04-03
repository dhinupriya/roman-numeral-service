package com.adobe.romannumeral.infrastructure.observability;

import com.adobe.romannumeral.domain.model.RomanNumeralResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * Business-level metrics for Roman numeral conversions.
 *
 * <p>Uses Micrometer as a unified facade — metrics are shipped to whatever backend
 * is configured (Prometheus for Grafana dashboards in our case). If the ops team
 * switches to Datadog tomorrow, zero code changes — just swap the registry dependency.
 *
 * <p>Recorded metrics:
 * <ul>
 *   <li>{@code roman_conversion_single_total} — counter of single conversions</li>
 *   <li>{@code roman_conversion_single_duration_seconds} — timer for single conversion latency</li>
 *   <li>{@code roman_conversion_range_total} — counter of range conversions</li>
 *   <li>{@code roman_conversion_range_duration_seconds} — timer for range conversion latency</li>
 *   <li>{@code roman_conversion_range_size} — distribution of range sizes requested</li>
 *   <li>{@code roman_conversion_error_total} — counter of errors, tagged by type</li>
 * </ul>
 *
 * <p>This class is a collaborator injected into use cases and the error handler,
 * not a decorator or AOP aspect. Each class retains a single responsibility:
 * use cases orchestrate business logic, ConversionMetrics records metrics.
 */
@Slf4j
@Component
public class ConversionMetrics {

    private final Timer singleTimer;
    private final Counter singleCounter;
    private final Timer rangeTimer;
    private final Counter rangeCounter;
    private final DistributionSummary rangeSizeSummary;
    private final MeterRegistry registry;

    public ConversionMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.singleCounter = Counter.builder("roman_conversion_single_total")
                .description("Total number of single conversions")
                .register(registry);

        this.singleTimer = Timer.builder("roman_conversion_single_duration_seconds")
                .description("Duration of single conversions")
                .register(registry);

        this.rangeCounter = Counter.builder("roman_conversion_range_total")
                .description("Total number of range conversions")
                .register(registry);

        this.rangeTimer = Timer.builder("roman_conversion_range_duration_seconds")
                .description("Duration of range conversions")
                .register(registry);

        this.rangeSizeSummary = DistributionSummary.builder("roman_conversion_range_size")
                .description("Distribution of range sizes requested")
                .register(registry);
    }

    /**
     * Records a single conversion — increments counter and records duration.
     *
     * @param conversion the conversion to execute and measure
     * @return the conversion result
     */
    public RomanNumeralResult recordSingle(Supplier<RomanNumeralResult> conversion) {
        singleCounter.increment();
        return singleTimer.record(conversion);
    }

    /**
     * Records a range conversion — increments counter, records duration and range size.
     *
     * @param rangeSize  the number of values in the range
     * @param conversion the conversion to execute and measure
     * @return the list of conversion results
     */
    public List<RomanNumeralResult> recordRange(int rangeSize, Supplier<List<RomanNumeralResult>> conversion) {
        rangeCounter.increment();
        rangeSizeSummary.record(rangeSize);
        return rangeTimer.record(conversion);
    }

    /**
     * Records a conversion error, tagged by exception type.
     * Called from GlobalExceptionHandler — the single place all errors flow through.
     *
     * @param errorType the type of error (e.g., "InvalidInput", "InvalidRange")
     */
    public void recordError(String errorType) {
        Counter.builder("roman_conversion_error_total")
                .description("Total number of conversion errors")
                .tag("type", errorType)
                .register(registry)
                .increment();
    }
}

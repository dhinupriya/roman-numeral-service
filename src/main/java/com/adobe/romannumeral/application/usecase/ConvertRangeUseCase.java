package com.adobe.romannumeral.application.usecase;

import com.adobe.romannumeral.application.port.ParallelExecutionPort;
import com.adobe.romannumeral.domain.model.RomanNumeralRange;
import com.adobe.romannumeral.domain.model.RomanNumeralResult;
import com.adobe.romannumeral.domain.service.RomanNumeralConverter;
import com.adobe.romannumeral.infrastructure.observability.ConversionMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Use case for converting a range of integers to Roman numerals in parallel.
 *
 * <p>Orchestrates the range conversion workflow:
 * <ol>
 *   <li>Receive a validated {@link RomanNumeralRange}</li>
 *   <li>Delegate to {@link ParallelExecutionPort} for chunked parallel execution</li>
 *   <li>Each chunk calls {@link RomanNumeralConverter#convert(int)} (the algorithm)</li>
 *   <li>Results are assembled in ascending order</li>
 * </ol>
 *
 * <p>Records business metrics via {@link ConversionMetrics} (counter + timer + range size).
 *
 * <p>Uses {@code StandardRomanNumeralConverter} (not cached) because the assessment
 * requires <em>"use multithreading to compute the values in the range in parallel"</em>
 * — real computation at request time, not cache reads.
 */
@Slf4j
@RequiredArgsConstructor
public class ConvertRangeUseCase {

    private final RomanNumeralConverter converter;
    private final ParallelExecutionPort parallelExecutor;
    private final ConversionMetrics metrics;

    /**
     * Converts all integers in the range to Roman numerals using parallel computation.
     * Records conversion count, duration, and range size as business metrics.
     *
     * @param range a validated range (min &lt; max, both in bounds)
     * @return ordered list of conversion results from min to max
     */
    public List<RomanNumeralResult> execute(RomanNumeralRange range) {
        log.debug("Converting range [{}-{}] ({} values)",
                range.min(), range.max(), range.size());

        return metrics.recordRange(range.size(), () ->
                parallelExecutor.executeRange(
                        range.min(),
                        range.max(),
                        converter::convert));
    }
}

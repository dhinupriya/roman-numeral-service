package com.adobe.romannumeral.application.usecase;

import com.adobe.romannumeral.domain.model.RomanNumeralResult;
import com.adobe.romannumeral.domain.service.RomanNumeralConverter;
import com.adobe.romannumeral.infrastructure.observability.ConversionMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Use case for converting a single integer to a Roman numeral.
 *
 * <p>Delegates to {@code CachedRomanNumeralConverter} for O(1) lookups.
 * Records business metrics via {@link ConversionMetrics} (counter + timer).
 *
 * <p>Single Responsibility: this use case handles only single conversions.
 * Range conversions are handled by {@link ConvertRangeUseCase}.
 */
@Slf4j
@RequiredArgsConstructor
public class ConvertSingleNumberUseCase {

    private final RomanNumeralConverter converter;
    private final ConversionMetrics metrics;

    /**
     * Converts a single integer to its Roman numeral representation.
     * Records conversion count and duration as business metrics.
     *
     * @param number the integer to convert
     * @return the conversion result
     * @throws com.adobe.romannumeral.domain.exception.InvalidInputException if out of range
     */
    public RomanNumeralResult execute(int number) {
        log.debug("Converting single number: {}", number);
        return metrics.recordSingle(() -> converter.convert(number));
    }
}

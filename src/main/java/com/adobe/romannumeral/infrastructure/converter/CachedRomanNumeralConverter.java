package com.adobe.romannumeral.infrastructure.converter;

import com.adobe.romannumeral.domain.exception.InvalidInputException;
import com.adobe.romannumeral.domain.model.RomanNumeralResult;
import com.adobe.romannumeral.domain.service.RomanNumeralConstants;
import com.adobe.romannumeral.domain.service.RomanNumeralConverter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Cached Roman numeral converter — O(1) lookup for single queries.
 *
 * <p>Implements the <b>Decorator pattern</b>: wraps {@link StandardRomanNumeralConverter}
 * and adds a caching layer. At startup, pre-computes all 3999 Roman numeral values
 * into a {@code String[]} array using the wrapped converter.
 *
 * <p><b>Why pre-compute into an array?</b>
 * <ul>
 *   <li>Keys are contiguous integers 1-3999 — perfect for array indexing</li>
 *   <li>Array access: ~1ns (CPU cache-friendly, no hashing)</li>
 *   <li>HashMap access: ~10-50ns (hash computation + potential collisions)</li>
 *   <li>3999 String references = minimal memory footprint</li>
 * </ul>
 *
 * <p><b>Why sequential startup, not parallel?</b>
 * 3999 conversions × ~1μs = ~4ms total. Spring Boot startup takes ~2-3 seconds.
 * Parallelizing a 4ms task adds complexity for negligible gain (YAGNI).
 *
 * <p><b>Thread safety:</b> The array is effectively immutable after construction.
 * Populated during {@code @PostConstruct} (single-threaded, before any request),
 * read-only during request handling. Java Memory Model guarantees visibility via
 * the happens-before relationship established by Spring's bean initialization.
 *
 * <p>Used for <b>single queries</b> ({@code ?query=42}). Range queries use
 * {@link StandardRomanNumeralConverter} directly for real parallel computation.
 */
@Slf4j
public class CachedRomanNumeralConverter implements RomanNumeralConverter {

    private final StandardRomanNumeralConverter delegate;
    private final String[] cache;

    public CachedRomanNumeralConverter(StandardRomanNumeralConverter delegate) {
        this.delegate = delegate;
        this.cache = new String[RomanNumeralConstants.MAX_VALUE + 1];
    }

    /**
     * Pre-computes all Roman numeral values at startup.
     * Sequential loop — ~4ms total, negligible compared to Spring Boot's startup time.
     */
    @PostConstruct
    public void initializeCache() {
        long start = System.nanoTime();
        for (int i = RomanNumeralConstants.MIN_VALUE; i <= RomanNumeralConstants.MAX_VALUE; i++) {
            cache[i] = delegate.convert(i).output();
        }
        long durationMs = (System.nanoTime() - start) / 1_000_000;
        log.info("Pre-computed {} Roman numeral values in {}ms", RomanNumeralConstants.MAX_VALUE, durationMs);
    }

    /**
     * O(1) array lookup — returns the pre-computed result instantly.
     */
    @Override
    public RomanNumeralResult convert(int number) {
        if (number < RomanNumeralConstants.MIN_VALUE || number > RomanNumeralConstants.MAX_VALUE) {
            throw new InvalidInputException(
                    "Number must be between " + RomanNumeralConstants.MIN_VALUE
                            + " and " + RomanNumeralConstants.MAX_VALUE + ", got: " + number);
        }
        return new RomanNumeralResult(String.valueOf(number), cache[number]);
    }
}

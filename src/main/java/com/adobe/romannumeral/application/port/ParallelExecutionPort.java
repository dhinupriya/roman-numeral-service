package com.adobe.romannumeral.application.port;

import com.adobe.romannumeral.domain.model.RomanNumeralResult;

import java.util.List;
import java.util.function.Function;

/**
 * Port (interface) for parallel execution of range conversions.
 *
 * <p>Abstracts the threading strategy from the use case. The application layer
 * says "execute this function over this range in parallel" — the infrastructure
 * layer decides how (chunked virtual threads, thread pool, etc.).
 *
 * <p>This separation allows swapping the threading strategy without touching
 * business logic. For example, switching from chunked virtual threads to
 * a fixed thread pool would only require a new adapter implementation.
 */
public interface ParallelExecutionPort {

    /**
     * Executes a conversion function over a range of integers in parallel,
     * returning results in ascending order.
     *
     * @param startInclusive the first number in the range
     * @param endInclusive   the last number in the range
     * @param converter      the function to apply to each number
     * @return ordered list of results from startInclusive to endInclusive
     */
    List<RomanNumeralResult> executeRange(
            int startInclusive,
            int endInclusive,
            Function<Integer, RomanNumeralResult> converter);
}

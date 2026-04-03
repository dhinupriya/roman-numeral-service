package com.adobe.romannumeral.infrastructure.config;

import com.adobe.romannumeral.application.port.ParallelExecutionPort;
import com.adobe.romannumeral.application.usecase.ConvertRangeUseCase;
import com.adobe.romannumeral.application.usecase.ConvertSingleNumberUseCase;
import com.adobe.romannumeral.infrastructure.converter.CachedRomanNumeralConverter;
import com.adobe.romannumeral.infrastructure.converter.StandardRomanNumeralConverter;
import com.adobe.romannumeral.infrastructure.execution.ChunkedParallelExecutor;
import com.adobe.romannumeral.infrastructure.observability.ConversionMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit bean wiring for converters and use cases.
 *
 * <p>A principal engineer decision: explicit wiring over {@code @Primary/@Qualifier} annotations.
 * Every wiring decision is in one place — a new developer opens this file and immediately
 * understands which converter each use case gets:
 * <ul>
 *   <li>Single queries → {@link CachedRomanNumeralConverter} (O(1) lookup)</li>
 *   <li>Range queries → {@link StandardRomanNumeralConverter} (real parallel computation)</li>
 * </ul>
 *
 * <p>No annotation hunting required. Readable, debuggable, one source of truth for wiring.
 */
@Configuration
public class ConverterConfig {

    @Bean
    public StandardRomanNumeralConverter standardConverter() {
        return new StandardRomanNumeralConverter();
    }

    @Bean
    public CachedRomanNumeralConverter cachedConverter(StandardRomanNumeralConverter standardConverter) {
        return new CachedRomanNumeralConverter(standardConverter);
    }

    @Bean
    public ParallelExecutionPort parallelExecutor() {
        return new ChunkedParallelExecutor();
    }

    @Bean
    public ConvertSingleNumberUseCase convertSingleNumberUseCase(
            CachedRomanNumeralConverter cachedConverter,
            ConversionMetrics metrics) {
        return new ConvertSingleNumberUseCase(cachedConverter, metrics);
    }

    @Bean
    public ConvertRangeUseCase convertRangeUseCase(
            StandardRomanNumeralConverter standardConverter,
            ParallelExecutionPort parallelExecutor,
            ConversionMetrics metrics) {
        return new ConvertRangeUseCase(standardConverter, parallelExecutor, metrics);
    }
}

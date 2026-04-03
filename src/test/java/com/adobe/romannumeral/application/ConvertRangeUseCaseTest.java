package com.adobe.romannumeral.application;

import com.adobe.romannumeral.application.port.ParallelExecutionPort;
import com.adobe.romannumeral.application.usecase.ConvertRangeUseCase;
import com.adobe.romannumeral.domain.model.RomanNumeralRange;
import com.adobe.romannumeral.domain.model.RomanNumeralResult;
import com.adobe.romannumeral.domain.service.RomanNumeralConverter;
import com.adobe.romannumeral.infrastructure.observability.ConversionMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ConvertRangeUseCase — verifies delegation, metrics, and function passing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConvertRangeUseCase")
class ConvertRangeUseCaseTest {

    @Mock
    private RomanNumeralConverter converter;

    @Mock
    private ParallelExecutionPort parallelExecutor;

    @Mock
    private ConversionMetrics metrics;

    @InjectMocks
    private ConvertRangeUseCase useCase;

    @Test
    @DisplayName("Should delegate to parallel executor and record metrics")
    @SuppressWarnings("unchecked")
    void shouldDelegateAndRecordMetrics() {
        var range = new RomanNumeralRange(1, 10);
        var expectedResults = List.of(
                new RomanNumeralResult("1", "I"),
                new RomanNumeralResult("2", "II"));

        when(metrics.recordRange(eq(10), any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<List<RomanNumeralResult>> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(parallelExecutor.executeRange(eq(1), eq(10), any()))
                .thenReturn(expectedResults);

        List<RomanNumeralResult> results = useCase.execute(range);

        assertThat(results).isEqualTo(expectedResults);
        verify(metrics).recordRange(eq(10), any(Supplier.class));
        verify(parallelExecutor).executeRange(eq(1), eq(10), any());
    }

    @Test
    @DisplayName("Should pass converter::convert as the conversion function")
    @SuppressWarnings("unchecked")
    void shouldPassConverterFunction() {
        var range = new RomanNumeralRange(1, 3);
        when(metrics.recordRange(anyInt(), any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<List<RomanNumeralResult>> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(parallelExecutor.executeRange(anyInt(), anyInt(), any()))
                .thenReturn(List.of());

        useCase.execute(range);

        ArgumentCaptor<Function<Integer, RomanNumeralResult>> captor =
                ArgumentCaptor.forClass(Function.class);
        verify(parallelExecutor).executeRange(eq(1), eq(3), captor.capture());

        Function<Integer, RomanNumeralResult> capturedFn = captor.getValue();
        var mockResult = new RomanNumeralResult("1", "I");
        when(converter.convert(1)).thenReturn(mockResult);
        assertThat(capturedFn.apply(1)).isEqualTo(mockResult);
    }
}

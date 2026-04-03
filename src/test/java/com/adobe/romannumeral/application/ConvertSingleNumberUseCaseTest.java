package com.adobe.romannumeral.application;

import com.adobe.romannumeral.application.usecase.ConvertSingleNumberUseCase;
import com.adobe.romannumeral.domain.exception.InvalidInputException;
import com.adobe.romannumeral.domain.model.RomanNumeralResult;
import com.adobe.romannumeral.domain.service.RomanNumeralConverter;
import com.adobe.romannumeral.infrastructure.observability.ConversionMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConvertSingleNumberUseCase — verifies delegation, metrics, and error propagation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConvertSingleNumberUseCase")
class ConvertSingleNumberUseCaseTest {

    @Mock
    private RomanNumeralConverter converter;

    @Mock
    private ConversionMetrics metrics;

    @InjectMocks
    private ConvertSingleNumberUseCase useCase;

    @Test
    @DisplayName("Should delegate to converter and record metrics")
    @SuppressWarnings("unchecked")
    void shouldDelegateToConverterAndRecordMetrics() {
        var expected = new RomanNumeralResult("42", "XLII");
        when(metrics.recordSingle(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<RomanNumeralResult> supplier = invocation.getArgument(0);
            return supplier.get();
        });
        when(converter.convert(42)).thenReturn(expected);

        RomanNumeralResult result = useCase.execute(42);

        assertThat(result).isEqualTo(expected);
        verify(metrics).recordSingle(any(Supplier.class));
    }

    @Test
    @DisplayName("Should propagate InvalidInputException from converter")
    @SuppressWarnings("unchecked")
    void shouldPropagateException() {
        when(metrics.recordSingle(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<RomanNumeralResult> supplier = invocation.getArgument(0);
            return supplier.get();
        });
        when(converter.convert(0))
                .thenThrow(new InvalidInputException("Number must be between 1 and 3999, got: 0"));

        assertThatThrownBy(() -> useCase.execute(0))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("got: 0");
    }
}

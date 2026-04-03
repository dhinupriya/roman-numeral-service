package com.adobe.romannumeral.application;

import com.adobe.romannumeral.application.usecase.ConvertSingleNumberUseCase;
import com.adobe.romannumeral.domain.exception.InvalidInputException;
import com.adobe.romannumeral.domain.model.RomanNumeralResult;
import com.adobe.romannumeral.domain.service.RomanNumeralConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ConvertSingleNumberUseCase — verifies delegation and error propagation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConvertSingleNumberUseCase")
class ConvertSingleNumberUseCaseTest {

    @Mock
    private RomanNumeralConverter converter;

    @InjectMocks
    private ConvertSingleNumberUseCase useCase;

    @Test
    @DisplayName("Should delegate to converter with correct argument")
    void shouldDelegateToConverter() {
        var expected = new RomanNumeralResult("42", "XLII");
        when(converter.convert(42)).thenReturn(expected);

        RomanNumeralResult result = useCase.execute(42);

        assertThat(result).isEqualTo(expected);
        verify(converter).convert(42);
    }

    @Test
    @DisplayName("Should propagate InvalidInputException from converter")
    void shouldPropagateException() {
        when(converter.convert(0))
                .thenThrow(new InvalidInputException("Number must be between 1 and 3999, got: 0"));

        assertThatThrownBy(() -> useCase.execute(0))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("got: 0");
    }
}

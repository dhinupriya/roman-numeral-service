package com.adobe.romannumeral.domain;

import com.adobe.romannumeral.domain.exception.InvalidInputException;
import com.adobe.romannumeral.domain.model.RomanNumeralResult;
import com.adobe.romannumeral.infrastructure.converter.StandardRomanNumeralConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive tests for the Roman numeral conversion algorithm.
 *
 * <p>Covers: all 7 standard symbols, all 6 subtractive forms, boundary values,
 * compound numbers, largest output, and invalid inputs.
 */
@DisplayName("StandardRomanNumeralConverter")
class StandardRomanNumeralConverterTest {

    private final StandardRomanNumeralConverter converter = new StandardRomanNumeralConverter();

    @Nested
    @DisplayName("Standard symbols")
    class StandardSymbols {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "1, I",
                "5, V",
                "10, X",
                "50, L",
                "100, C",
                "500, D",
                "1000, M"
        })
        void shouldConvertStandardSymbols(int input, String expected) {
            RomanNumeralResult result = converter.convert(input);
            assertThat(result.output()).isEqualTo(expected);
            assertThat(result.input()).isEqualTo(String.valueOf(input));
        }
    }

    @Nested
    @DisplayName("Subtractive forms")
    class SubtractiveForms {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "4, IV",
                "9, IX",
                "40, XL",
                "90, XC",
                "400, CD",
                "900, CM"
        })
        void shouldConvertSubtractiveForms(int input, String expected) {
            assertThat(converter.convert(input).output()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Compound numbers")
    class CompoundNumbers {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "2, II",
                "3, III",
                "14, XIV",
                "42, XLII",
                "99, XCIX",
                "246, CCXLVI",
                "789, DCCLXXXIX",
                "1066, MLXVI",
                "1994, MCMXCIV",
                "2024, MMXXIV",
                "3549, MMMDXLIX"
        })
        void shouldConvertCompoundNumbers(int input, String expected) {
            assertThat(converter.convert(input).output()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Boundary values")
    class BoundaryValues {

        @Test
        @DisplayName("Minimum value: 1 → I")
        void shouldConvertMinimum() {
            assertThat(converter.convert(1).output()).isEqualTo("I");
        }

        @Test
        @DisplayName("Maximum value: 3999 → MMMCMXCIX")
        void shouldConvertMaximum() {
            assertThat(converter.convert(3999).output()).isEqualTo("MMMCMXCIX");
        }

        @Test
        @DisplayName("Largest output: 3888 → MMMDCCCLXXXVIII (15 characters)")
        void shouldConvertLargestOutput() {
            String result = converter.convert(3888).output();
            assertThat(result).isEqualTo("MMMDCCCLXXXVIII");
            assertThat(result).hasSize(15);
        }
    }

    @Nested
    @DisplayName("Adjacent to boundaries")
    class AdjacentToBoundaries {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "2, II",
                "3998, MMMCMXCVIII"
        })
        void shouldConvertValuesAdjacentToBoundaries(int input, String expected) {
            assertThat(converter.convert(input).output()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Multiple subtractive forms and all symbols")
    class ComplexCombinations {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "1444, MCDXLIV",
                "999, CMXCIX",
                "1666, MDCLXVI",
                "494, CDXCIV",
                "3999, MMMCMXCIX"
        })
        void shouldHandleComplexCombinations(int input, String expected) {
            assertThat(converter.convert(input).output()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Subtractive threshold edges")
    class SubtractiveThresholdEdges {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "3, III",
                "8, VIII",
                "39, XXXIX",
                "89, LXXXIX",
                "399, CCCXCIX",
                "899, DCCCXCIX"
        })
        void shouldNotUseSubtractiveFormBelowThreshold(int input, String expected) {
            assertThat(converter.convert(input).output()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Repeated symbols")
    class RepeatedSymbols {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "2, II",
                "3, III",
                "20, XX",
                "30, XXX",
                "200, CC",
                "300, CCC",
                "2000, MM",
                "3000, MMM"
        })
        void shouldRepeatSymbolsCorrectly(int input, String expected) {
            assertThat(converter.convert(input).output()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Invalid inputs")
    class InvalidInputs {

        @ParameterizedTest(name = "Should reject {0}")
        @ValueSource(ints = {0, -1, -100, Integer.MIN_VALUE, 4000, 5000, Integer.MAX_VALUE})
        void shouldRejectOutOfRangeValues(int input) {
            assertThatThrownBy(() -> converter.convert(input))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessageContaining("must be between");
        }
    }

    @Nested
    @DisplayName("Result format")
    class ResultFormat {

        @Test
        @DisplayName("Input field should be string representation of the number")
        void shouldReturnInputAsString() {
            RomanNumeralResult result = converter.convert(1994);
            assertThat(result.input()).isEqualTo("1994");
        }

        @Test
        @DisplayName("Output field should be the Roman numeral string")
        void shouldReturnOutputAsRomanNumeral() {
            RomanNumeralResult result = converter.convert(1994);
            assertThat(result.output()).isEqualTo("MCMXCIV");
        }
    }

    @Nested
    @DisplayName("Idempotency")
    class Idempotency {

        @Test
        @DisplayName("Calling convert() twice should return identical results")
        void shouldBeIdempotent() {
            RomanNumeralResult first = converter.convert(1994);
            RomanNumeralResult second = converter.convert(1994);
            assertThat(first).isEqualTo(second);
        }
    }

    @Nested
    @DisplayName("Thread safety")
    class ThreadSafety {

        @Test
        @DisplayName("Concurrent calls from multiple threads should all return correct results")
        void shouldBeThreadSafe() throws InterruptedException {
            AtomicBoolean failed = new AtomicBoolean(false);
            try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
                for (int i = 0; i < 100; i++) {
                    final int number = (i % 3999) + 1; // cycle through 1-3999
                    executor.submit(() -> {
                        try {
                            RomanNumeralResult result = converter.convert(number);
                            if (!result.input().equals(String.valueOf(number))) {
                                failed.set(true);
                            }
                            if (result.output() == null || result.output().isEmpty()) {
                                failed.set(true);
                            }
                        } catch (Exception e) {
                            failed.set(true);
                        }
                    });
                }
                executor.shutdown();
                executor.awaitTermination(10, TimeUnit.SECONDS);
            }
            assertThat(failed.get()).isFalse();
        }
    }

    @Nested
    @DisplayName("Error message content")
    class ErrorMessageContent {

        @Test
        @DisplayName("Error for below-range should include the invalid value")
        void shouldIncludeInvalidValueInErrorMessage() {
            assertThatThrownBy(() -> converter.convert(0))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessageContaining("got: 0");
        }

        @Test
        @DisplayName("Error for above-range should include the invalid value")
        void shouldIncludeAboveRangeValueInErrorMessage() {
            assertThatThrownBy(() -> converter.convert(4000))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessageContaining("got: 4000");
        }
    }

    @Nested
    @DisplayName("Exhaustive correctness")
    class ExhaustiveCorrectness {

        @Test
        @DisplayName("Every number 1-3999 should produce a non-empty Roman numeral")
        void shouldProduceNonEmptyOutputForAllValidNumbers() {
            for (int i = 1; i <= 3999; i++) {
                RomanNumeralResult result = converter.convert(i);
                assertThat(result.output())
                        .as("convert(%d) should produce non-empty output", i)
                        .isNotNull()
                        .isNotEmpty();
                assertThat(result.input()).isEqualTo(String.valueOf(i));
            }
        }
    }
}

package com.adobe.romannumeral.domain;

import com.adobe.romannumeral.domain.exception.InvalidInputException;
import com.adobe.romannumeral.infrastructure.converter.CachedRomanNumeralConverter;
import com.adobe.romannumeral.infrastructure.converter.StandardRomanNumeralConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the CachedRomanNumeralConverter — verifies cache correctness
 * and consistency with the standard algorithm.
 */
@DisplayName("CachedRomanNumeralConverter")
class CachedRomanNumeralConverterTest {

    private static final StandardRomanNumeralConverter standard = new StandardRomanNumeralConverter();
    private static CachedRomanNumeralConverter cached;

    @BeforeAll
    static void initCache() {
        cached = new CachedRomanNumeralConverter(standard);
        cached.initializeCache();
    }

    @Nested
    @DisplayName("Cache correctness")
    class CacheCorrectness {

        @Test
        @DisplayName("Cached result should match standard algorithm for all 3999 values")
        void shouldMatchStandardForAllValues() {
            for (int i = 1; i <= 3999; i++) {
                var cachedResult = cached.convert(i);
                var standardResult = standard.convert(i);
                assertThat(cachedResult.output())
                        .as("Mismatch at %d: cached=%s, standard=%s",
                                i, cachedResult.output(), standardResult.output())
                        .isEqualTo(standardResult.output());
                assertThat(cachedResult.input()).isEqualTo(standardResult.input());
            }
        }
    }

    @Nested
    @DisplayName("O(1) lookup")
    class LookupBehavior {

        @Test
        @DisplayName("convert(1) should return I from cache")
        void shouldReturnFromCache() {
            var result = cached.convert(1);
            assertThat(result.input()).isEqualTo("1");
            assertThat(result.output()).isEqualTo("I");
        }

        @Test
        @DisplayName("convert(3999) should return MMMCMXCIX from cache")
        void shouldReturnMaxFromCache() {
            var result = cached.convert(3999);
            assertThat(result.input()).isEqualTo("3999");
            assertThat(result.output()).isEqualTo("MMMCMXCIX");
        }

        @Test
        @DisplayName("Multiple calls should return identical results (idempotent)")
        void shouldBeIdempotent() {
            var first = cached.convert(42);
            var second = cached.convert(42);
            assertThat(first).isEqualTo(second);
        }
    }

    @Nested
    @DisplayName("Input validation")
    class InputValidation {

        @Test
        @DisplayName("Should reject 0")
        void shouldRejectZero() {
            assertThatThrownBy(() -> cached.convert(0))
                    .isInstanceOf(InvalidInputException.class);
        }

        @Test
        @DisplayName("Should reject 4000")
        void shouldRejectAboveMax() {
            assertThatThrownBy(() -> cached.convert(4000))
                    .isInstanceOf(InvalidInputException.class);
        }

        @Test
        @DisplayName("Should reject negative numbers")
        void shouldRejectNegative() {
            assertThatThrownBy(() -> cached.convert(-1))
                    .isInstanceOf(InvalidInputException.class);
        }
    }
}

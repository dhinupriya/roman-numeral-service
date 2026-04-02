package com.adobe.romannumeral.domain;

import com.adobe.romannumeral.domain.exception.InvalidRangeException;
import com.adobe.romannumeral.domain.model.RomanNumeralRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the self-validating RomanNumeralRange Value Object.
 */
@DisplayName("RomanNumeralRange")
class RomanNumeralRangeTest {

    @Nested
    @DisplayName("Valid ranges")
    class ValidRanges {

        @Test
        @DisplayName("Should accept valid range 1-10")
        void shouldAcceptValidRange() {
            var range = new RomanNumeralRange(1, 10);
            assertThat(range.min()).isEqualTo(1);
            assertThat(range.max()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should accept full range 1-3999")
        void shouldAcceptFullRange() {
            var range = new RomanNumeralRange(1, 3999);
            assertThat(range.size()).isEqualTo(3999);
        }

        @Test
        @DisplayName("Should accept minimum valid range (adjacent numbers)")
        void shouldAcceptMinimumRange() {
            var range = new RomanNumeralRange(1, 2);
            assertThat(range.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("Size should be max - min + 1")
        void shouldCalculateSizeCorrectly() {
            assertThat(new RomanNumeralRange(5, 15).size()).isEqualTo(11);
        }
    }

    @Nested
    @DisplayName("Invalid ranges — min >= max")
    class MinGreaterThanOrEqualMax {

        @Test
        @DisplayName("Should reject min == max")
        void shouldRejectEqualMinMax() {
            assertThatThrownBy(() -> new RomanNumeralRange(5, 5))
                    .isInstanceOf(InvalidRangeException.class)
                    .hasMessageContaining("min must be less than max");
        }

        @Test
        @DisplayName("Should reject min > max")
        void shouldRejectReversedRange() {
            assertThatThrownBy(() -> new RomanNumeralRange(10, 5))
                    .isInstanceOf(InvalidRangeException.class)
                    .hasMessageContaining("min must be less than max");
        }
    }

    @Nested
    @DisplayName("Invalid ranges — out of bounds")
    class OutOfBounds {

        @Test
        @DisplayName("Should reject min = 0")
        void shouldRejectZeroMin() {
            assertThatThrownBy(() -> new RomanNumeralRange(0, 10))
                    .isInstanceOf(InvalidRangeException.class)
                    .hasMessageContaining("min must be between");
        }

        @Test
        @DisplayName("Should reject negative min")
        void shouldRejectNegativeMin() {
            assertThatThrownBy(() -> new RomanNumeralRange(-1, 10))
                    .isInstanceOf(InvalidRangeException.class)
                    .hasMessageContaining("min must be between");
        }

        @Test
        @DisplayName("Should reject max > 3999")
        void shouldRejectMaxAboveLimit() {
            assertThatThrownBy(() -> new RomanNumeralRange(1, 4000))
                    .isInstanceOf(InvalidRangeException.class)
                    .hasMessageContaining("max must be between");
        }

        @Test
        @DisplayName("Should reject min > 3999")
        void shouldRejectMinAboveLimit() {
            assertThatThrownBy(() -> new RomanNumeralRange(4000, 5000))
                    .isInstanceOf(InvalidRangeException.class)
                    .hasMessageContaining("min must be between");
        }
    }
}

package com.adobe.romannumeral.domain;

import com.adobe.romannumeral.domain.model.RomanNumeralResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the RomanNumeralResult Value Object — null safety and equality.
 */
@DisplayName("RomanNumeralResult")
class RomanNumeralResultTest {

    @Test
    @DisplayName("Should create valid result")
    void shouldCreateValidResult() {
        var result = new RomanNumeralResult("42", "XLII");
        assertThat(result.input()).isEqualTo("42");
        assertThat(result.output()).isEqualTo("XLII");
    }

    @Test
    @DisplayName("Should reject null input")
    void shouldRejectNullInput() {
        assertThatThrownBy(() -> new RomanNumeralResult(null, "I"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("input must not be null");
    }

    @Test
    @DisplayName("Should reject null output")
    void shouldRejectNullOutput() {
        assertThatThrownBy(() -> new RomanNumeralResult("1", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("output must not be null");
    }

    @Test
    @DisplayName("Equal results should be equal")
    void shouldBeEqualForSameValues() {
        var result1 = new RomanNumeralResult("1", "I");
        var result2 = new RomanNumeralResult("1", "I");
        assertThat(result1).isEqualTo(result2);
    }

    @Test
    @DisplayName("Different results should not be equal")
    void shouldNotBeEqualForDifferentValues() {
        var result1 = new RomanNumeralResult("1", "I");
        var result2 = new RomanNumeralResult("2", "II");
        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    @DisplayName("Should reject both null")
    void shouldRejectBothNull() {
        assertThatThrownBy(() -> new RomanNumeralResult(null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("input must not be null");
    }

    @Test
    @DisplayName("Equal results should have same hashCode")
    void shouldHaveConsistentHashCode() {
        var result1 = new RomanNumeralResult("1", "I");
        var result2 = new RomanNumeralResult("1", "I");
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }
}

package com.adobe.romannumeral.web;

import com.adobe.romannumeral.application.usecase.ConvertRangeUseCase;
import com.adobe.romannumeral.application.usecase.ConvertSingleNumberUseCase;
import com.adobe.romannumeral.domain.exception.InvalidInputException;
import com.adobe.romannumeral.domain.model.RomanNumeralRange;
import com.adobe.romannumeral.domain.model.RomanNumeralResult;
import com.adobe.romannumeral.infrastructure.config.AppProperties;
import com.adobe.romannumeral.infrastructure.config.SecurityConfig;
import com.adobe.romannumeral.infrastructure.observability.ConversionMetrics;
import com.adobe.romannumeral.web.controller.RomanNumeralController;
import com.adobe.romannumeral.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API slice tests for the Roman numeral controller.
 *
 * <p>Uses {@code @WebMvcTest} — loads only the web layer (controller + exception handler),
 * mocks the use cases. Tests HTTP routing, JSON structure, validation, and error responses
 * for both single and range conversion endpoints.
 */
@WebMvcTest(
        controllers = RomanNumeralController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {
                        com.adobe.romannumeral.infrastructure.security.ApiKeyAuthFilter.class,
                        com.adobe.romannumeral.infrastructure.security.RateLimitFilter.class
                }
        )
)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@DisplayName("RomanNumeralController")
class RomanNumeralControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConvertSingleNumberUseCase convertSingleNumberUseCase;

    @MockitoBean
    private ConvertRangeUseCase convertRangeUseCase;

    @MockitoBean
    private AppProperties appProperties;

    @MockitoBean
    private ConversionMetrics conversionMetrics;

    @BeforeEach
    void setUp() {
        when(appProperties.getMaxRangeSize()).thenReturn(3999);
    }

    // ========================================================================
    // Single Conversion Tests
    // ========================================================================

    @Nested
    @DisplayName("Single conversion — valid inputs")
    class ValidSingleConversion {

        @Test
        @DisplayName("GET /romannumeral?query=1 → 200 with correct JSON")
        void shouldConvertValidNumber() throws Exception {
            when(convertSingleNumberUseCase.execute(1))
                    .thenReturn(new RomanNumeralResult("1", "I"));

            mockMvc.perform(get("/romannumeral").param("query", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.input").value("1"))
                    .andExpect(jsonPath("$.output").value("I"));
        }

        @Test
        @DisplayName("GET /romannumeral?query=1994 → 200 with MCMXCIV")
        void shouldConvertCompoundNumber() throws Exception {
            when(convertSingleNumberUseCase.execute(1994))
                    .thenReturn(new RomanNumeralResult("1994", "MCMXCIV"));

            mockMvc.perform(get("/romannumeral").param("query", "1994"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.input").value("1994"))
                    .andExpect(jsonPath("$.output").value("MCMXCIV"));
        }

        @Test
        @DisplayName("GET /romannumeral?query=3999 → 200 (max value)")
        void shouldConvertMaxValue() throws Exception {
            when(convertSingleNumberUseCase.execute(3999))
                    .thenReturn(new RomanNumeralResult("3999", "MMMCMXCIX"));

            mockMvc.perform(get("/romannumeral").param("query", "3999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.input").value("3999"))
                    .andExpect(jsonPath("$.output").value("MMMCMXCIX"));
        }
    }

    @Nested
    @DisplayName("Single conversion — invalid inputs")
    class InvalidSingleConversion {

        @Test
        @DisplayName("GET /romannumeral?query=0 → 400 (below range)")
        void shouldRejectZero() throws Exception {
            when(convertSingleNumberUseCase.execute(0))
                    .thenThrow(new InvalidInputException("Number must be between 1 and 3999, got: 0"));

            mockMvc.perform(get("/romannumeral").param("query", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("GET /romannumeral?query=abc → 400 (non-integer)")
        void shouldRejectNonInteger() throws Exception {
            mockMvc.perform(get("/romannumeral").param("query", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(
                            "'abc' is not a valid integer for parameter 'query'. "
                                    + "Must be a whole number between 1 and 3999"));
        }

        @Test
        @DisplayName("GET /romannumeral?query=1.5 → 400 (decimal)")
        void shouldRejectDecimal() throws Exception {
            mockMvc.perform(get("/romannumeral").param("query", "1.5"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }

        @Test
        @DisplayName("GET /romannumeral?query= → 400 (empty string)")
        void shouldRejectEmptyString() throws Exception {
            mockMvc.perform(get("/romannumeral").param("query", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Parameter 'query' must not be empty"));
        }

        @Test
        @DisplayName("GET /romannumeral?query=99999999999999 → 400 (overflow)")
        void shouldRejectOverflow() throws Exception {
            mockMvc.perform(get("/romannumeral").param("query", "99999999999999"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }
    }

    @Nested
    @DisplayName("Missing or ambiguous parameters")
    class MissingParameters {

        @Test
        @DisplayName("GET /romannumeral → 400 (no params at all)")
        void shouldRejectNoParams() throws Exception {
            mockMvc.perform(get("/romannumeral"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }

        @Test
        @DisplayName("GET /romannumeral?query=5&min=1&max=10 → 200 (query takes precedence)")
        void shouldPrioritizeQueryOverMinMax() throws Exception {
            when(convertSingleNumberUseCase.execute(5))
                    .thenReturn(new RomanNumeralResult("5", "V"));

            mockMvc.perform(get("/romannumeral")
                            .param("query", "5")
                            .param("min", "1")
                            .param("max", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.input").value("5"))
                    .andExpect(jsonPath("$.output").value("V"));
        }
    }

    @Nested
    @DisplayName("Partial range parameters")
    class PartialRangeParameters {

        @Test
        @DisplayName("GET /romannumeral?min=1 → 400 (missing max)")
        void shouldRejectMinWithoutMax() throws Exception {
            mockMvc.perform(get("/romannumeral").param("min", "1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }

        @Test
        @DisplayName("GET /romannumeral?max=10 → 400 (missing min)")
        void shouldRejectMaxWithoutMin() throws Exception {
            mockMvc.perform(get("/romannumeral").param("max", "10"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }
    }

    @Nested
    @DisplayName("Edge case inputs")
    class EdgeCaseInputs {

        @Test
        @DisplayName("GET /romannumeral?query=-1 → 400 (negative number)")
        void shouldRejectNegativeNumber() throws Exception {
            when(convertSingleNumberUseCase.execute(-1))
                    .thenThrow(new InvalidInputException("Number must be between 1 and 3999, got: -1"));

            mockMvc.perform(get("/romannumeral").param("query", "-1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }

        @Test
        @DisplayName("GET /romannumeral?query=  5   → 200 (whitespace trimmed)")
        void shouldTrimWhitespace() throws Exception {
            when(convertSingleNumberUseCase.execute(5))
                    .thenReturn(new RomanNumeralResult("5", "V"));

            mockMvc.perform(get("/romannumeral").param("query", "  5  "))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.output").value("V"));
        }

        @Test
        @DisplayName("GET /romannumeral?query=   → 400 (whitespace only)")
        void shouldRejectWhitespaceOnly() throws Exception {
            mockMvc.perform(get("/romannumeral").param("query", "   "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Parameter 'query' must not be empty"));
        }

        @Test
        @DisplayName("GET /romannumeral?query=007 → 200 (leading zeros parsed as 7)")
        void shouldHandleLeadingZeros() throws Exception {
            when(convertSingleNumberUseCase.execute(7))
                    .thenReturn(new RomanNumeralResult("7", "VII"));

            mockMvc.perform(get("/romannumeral").param("query", "007"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.output").value("VII"));
        }

        @Test
        @DisplayName("GET /romannumeral?query=<script> → 400 (XSS attempt, not reflected)")
        void shouldRejectAndNotReflectXssInput() throws Exception {
            mockMvc.perform(get("/romannumeral").param("query", "<script>alert(1)</script>"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.not(
                                    org.hamcrest.Matchers.containsString("<script>"))));
        }
    }

    @Nested
    @DisplayName("Long input sanitization")
    class LongInputSanitization {

        @Test
        @DisplayName("Input > 50 chars should be truncated in error message")
        void shouldTruncateLongInput() throws Exception {
            String longInput = "a".repeat(100);
            mockMvc.perform(get("/romannumeral").param("query", longInput))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("...")));
        }
    }

    @Nested
    @DisplayName("Integer boundary parsing")
    class IntegerBoundaryParsing {

        @Test
        @DisplayName("Integer.MAX_VALUE → parses, then domain rejects")
        void shouldRejectIntMaxValue() throws Exception {
            when(convertSingleNumberUseCase.execute(Integer.MAX_VALUE))
                    .thenThrow(new InvalidInputException(
                            "Number must be between 1 and 3999, got: " + Integer.MAX_VALUE));

            mockMvc.perform(get("/romannumeral").param("query", String.valueOf(Integer.MAX_VALUE)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Integer.MIN_VALUE → parses, then domain rejects")
        void shouldRejectIntMinValue() throws Exception {
            when(convertSingleNumberUseCase.execute(Integer.MIN_VALUE))
                    .thenThrow(new InvalidInputException(
                            "Number must be between 1 and 3999, got: " + Integer.MIN_VALUE));

            mockMvc.perform(get("/romannumeral").param("query", String.valueOf(Integer.MIN_VALUE)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================================================================
    // Range Conversion Tests (Phase 2)
    // ========================================================================

    @Nested
    @DisplayName("Range conversion — valid inputs")
    class ValidRangeConversion {

        @Test
        @DisplayName("GET /romannumeral?min=1&max=3 → 200 with 3 conversions")
        void shouldConvertValidRange() throws Exception {
            when(convertRangeUseCase.execute(any(RomanNumeralRange.class)))
                    .thenReturn(List.of(
                            new RomanNumeralResult("1", "I"),
                            new RomanNumeralResult("2", "II"),
                            new RomanNumeralResult("3", "III")));

            mockMvc.perform(get("/romannumeral")
                            .param("min", "1")
                            .param("max", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conversions").isArray())
                    .andExpect(jsonPath("$.conversions.length()").value(3))
                    .andExpect(jsonPath("$.conversions[0].input").value("1"))
                    .andExpect(jsonPath("$.conversions[0].output").value("I"))
                    .andExpect(jsonPath("$.conversions[1].input").value("2"))
                    .andExpect(jsonPath("$.conversions[1].output").value("II"))
                    .andExpect(jsonPath("$.conversions[2].input").value("3"))
                    .andExpect(jsonPath("$.conversions[2].output").value("III"));
        }

        @Test
        @DisplayName("Range response should have exactly 1 field (conversions)")
        void shouldReturnExactlyOneField() throws Exception {
            when(convertRangeUseCase.execute(any(RomanNumeralRange.class)))
                    .thenReturn(List.of(new RomanNumeralResult("1", "I")));

            mockMvc.perform(get("/romannumeral")
                            .param("min", "1")
                            .param("max", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conversions").exists())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("Range response Content-Type should be application/json")
        void shouldReturnJsonContentType() throws Exception {
            when(convertRangeUseCase.execute(any(RomanNumeralRange.class)))
                    .thenReturn(List.of(new RomanNumeralResult("1", "I")));

            mockMvc.perform(get("/romannumeral")
                            .param("min", "1")
                            .param("max", "2"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/json"));
        }
    }

    @Nested
    @DisplayName("Range conversion — invalid inputs")
    class InvalidRangeConversion {

        @Test
        @DisplayName("min > max → 400")
        void shouldRejectReversedRange() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("min", "10")
                            .param("max", "5"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("min must be less than max")));
        }

        @Test
        @DisplayName("min == max → 400")
        void shouldRejectEqualMinMax() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("min", "5")
                            .param("max", "5"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("min must be less than max")));
        }

        @Test
        @DisplayName("min=0 → 400 (below range)")
        void shouldRejectMinBelowRange() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("min", "0")
                            .param("max", "10"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("max=4000 → 400 (above range)")
        void shouldRejectMaxAboveRange() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("min", "1")
                            .param("max", "4000"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("min=abc → 400 (non-integer)")
        void shouldRejectNonIntegerMin() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("min", "abc")
                            .param("max", "10"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("parameter 'min'")));
        }

        @Test
        @DisplayName("max=xyz → 400 (non-integer)")
        void shouldRejectNonIntegerMax() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("min", "1")
                            .param("max", "xyz"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("parameter 'max'")));
        }

        @Test
        @DisplayName("Range exceeds max-range-size → 400")
        void shouldRejectOversizedRange() throws Exception {
            when(appProperties.getMaxRangeSize()).thenReturn(100);

            mockMvc.perform(get("/romannumeral")
                            .param("min", "1")
                            .param("max", "200"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("exceeds maximum")));
        }

        @Test
        @DisplayName("min=-5&max=10 → 400 (negative min)")
        void shouldRejectNegativeMin() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("min", "-5")
                            .param("max", "10"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("min=&max=10 → 400 (empty min)")
        void shouldRejectEmptyMin() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("min", "")
                            .param("max", "10"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Parameter 'min' must not be empty"));
        }

        @Test
        @DisplayName("min=1&max= → 400 (empty max)")
        void shouldRejectEmptyMax() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("min", "1")
                            .param("max", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Parameter 'max' must not be empty"));
        }
    }

    // ========================================================================
    // 500 Internal Server Error Tests
    // ========================================================================

    @Nested
    @DisplayName("Unexpected exceptions → 500")
    class UnexpectedExceptions {

        @Test
        @DisplayName("Unexpected RuntimeException from use case → 500 with generic message, no stack trace")
        void shouldReturn500ForUnexpectedException() throws Exception {
            when(convertSingleNumberUseCase.execute(1))
                    .thenThrow(new RuntimeException("Something broke internally"));

            mockMvc.perform(get("/romannumeral").param("query", "1"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("Internal Server Error"))
                    .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
                    .andExpect(jsonPath("$.status").value(500))
                    // Must NOT contain stack trace or internal details
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.not(
                                    org.hamcrest.Matchers.containsString("Something broke internally"))));
        }

        @Test
        @DisplayName("NullPointerException from use case → 500 with generic message")
        void shouldReturn500ForNullPointer() throws Exception {
            when(convertSingleNumberUseCase.execute(1))
                    .thenThrow(new NullPointerException());

            mockMvc.perform(get("/romannumeral").param("query", "1"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("Internal Server Error"))
                    .andExpect(jsonPath("$.status").value(500));
        }
    }

    // ========================================================================
    // HTTP Method & Path Tests
    // ========================================================================

    @Nested
    @DisplayName("HTTP method validation")
    class HttpMethodValidation {

        @Test
        @DisplayName("POST /romannumeral → 405")
        void shouldRejectPost() throws Exception {
            mockMvc.perform(post("/romannumeral").param("query", "1"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.error").value("Method Not Allowed"));
        }

        @Test
        @DisplayName("PUT /romannumeral → 405")
        void shouldRejectPut() throws Exception {
            mockMvc.perform(put("/romannumeral").param("query", "1"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("DELETE /romannumeral → 405")
        void shouldRejectDelete() throws Exception {
            mockMvc.perform(delete("/romannumeral").param("query", "1"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Nested
    @DisplayName("404 for unknown paths")
    class UnknownPaths {

        @Test
        @DisplayName("GET /unknown → 404")
        void shouldReturn404ForUnknownPath() throws Exception {
            mockMvc.perform(get("/unknown"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not Found"));
        }
    }

    // ========================================================================
    // Response Format Tests
    // ========================================================================

    @Nested
    @DisplayName("Response format validation")
    class ResponseFormatValidation {

        @Test
        @DisplayName("Success response Content-Type should be application/json")
        void shouldReturnJsonContentType() throws Exception {
            when(convertSingleNumberUseCase.execute(1))
                    .thenReturn(new RomanNumeralResult("1", "I"));

            mockMvc.perform(get("/romannumeral").param("query", "1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/json"));
        }

        @Test
        @DisplayName("Single success response should have exactly 2 fields")
        void shouldReturnExactlyTwoFieldsForSingle() throws Exception {
            when(convertSingleNumberUseCase.execute(1))
                    .thenReturn(new RomanNumeralResult("1", "I"));

            mockMvc.perform(get("/romannumeral").param("query", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.input").value("1"))
                    .andExpect(jsonPath("$.output").value("I"))
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("Error response should have exactly 3 fields")
        void shouldReturnExactlyThreeFieldsForError() throws Exception {
            mockMvc.perform(get("/romannumeral").param("query", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").isString())
                    .andExpect(jsonPath("$.message").isString())
                    .andExpect(jsonPath("$.status").isNumber())
                    .andExpect(jsonPath("$.length()").value(3));
        }

        @Test
        @DisplayName("Error response Content-Type should be application/json")
        void shouldReturnJsonContentTypeForErrors() throws Exception {
            mockMvc.perform(get("/romannumeral").param("query", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith("application/json"));
        }
    }
}

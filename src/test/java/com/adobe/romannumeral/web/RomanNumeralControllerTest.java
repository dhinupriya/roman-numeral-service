package com.adobe.romannumeral.web;

import com.adobe.romannumeral.application.usecase.ConvertSingleNumberUseCase;
import com.adobe.romannumeral.domain.exception.InvalidInputException;
import com.adobe.romannumeral.domain.model.RomanNumeralResult;
import com.adobe.romannumeral.infrastructure.config.SecurityConfig;
import com.adobe.romannumeral.web.controller.RomanNumeralController;
import com.adobe.romannumeral.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API slice tests for the Roman numeral controller.
 *
 * <p>Uses {@code @WebMvcTest} — loads only the web layer (controller + exception handler),
 * mocks the use case. Tests HTTP routing, JSON structure, validation, and error responses.
 */
@WebMvcTest(controllers = RomanNumeralController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@DisplayName("RomanNumeralController")
class RomanNumeralControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConvertSingleNumberUseCase convertSingleNumberUseCase;

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
                            "'abc' is not a valid integer. Must be a whole number between 1 and 3999"));
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
                    .andExpect(jsonPath("$.message").value("Parameter must not be empty"));
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
    @DisplayName("Missing or invalid parameters")
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
    @DisplayName("HTTP method validation")
    class HttpMethodValidation {

        @Test
        @DisplayName("POST /romannumeral → 405 (Method Not Allowed)")
        void shouldRejectPost() throws Exception {
            mockMvc.perform(post("/romannumeral").param("query", "1"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.error").value("Method Not Allowed"));
        }
    }

    @Nested
    @DisplayName("Error response format")
    class ErrorResponseFormat {

        @Test
        @DisplayName("Error response should have error, message, and status fields")
        void shouldReturnStructuredErrorResponse() throws Exception {
            mockMvc.perform(get("/romannumeral").param("query", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").isString())
                    .andExpect(jsonPath("$.message").isString())
                    .andExpect(jsonPath("$.status").isNumber());
        }
    }
}

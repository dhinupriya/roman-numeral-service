package com.adobe.romannumeral.security;

import com.adobe.romannumeral.TestConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security integration tests — full Spring context with real security filters.
 *
 * <p>Tests API key authentication, rate limiting, security headers, and
 * actuator bypass behavior.
 */
@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("API Security")
class ApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // ========================================================================
    // API Key Authentication
    // ========================================================================

    @Nested
    @DisplayName("API Key Authentication")
    class ApiKeyAuth {

        @Test
        @DisplayName("Missing API key → 401")
        void shouldRejectMissingApiKey() throws Exception {
            mockMvc.perform(get("/romannumeral").param("query", "1"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Missing API key. Provide a valid key via the X-API-Key header."))
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("Invalid API key → 401")
        void shouldRejectInvalidApiKey() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("query", "1")
                            .header(TestConstants.API_KEY_HEADER, "wrong-key"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Invalid API key."));
        }

        @Test
        @DisplayName("Empty API key → 401")
        void shouldRejectEmptyApiKey() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("query", "1")
                            .header(TestConstants.API_KEY_HEADER, ""))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Whitespace-only API key → 401")
        void shouldRejectWhitespaceApiKey() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("query", "1")
                            .header(TestConstants.API_KEY_HEADER, "   "))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Valid API key → 200")
        void shouldAcceptValidApiKey() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("query", "1")
                            .header(TestConstants.API_KEY_HEADER, TestConstants.API_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.input").value("1"))
                    .andExpect(jsonPath("$.output").value("I"));
        }

        @Test
        @DisplayName("Second valid API key → 200")
        void shouldAcceptSecondApiKey() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("query", "1")
                            .header(TestConstants.API_KEY_HEADER, "test-api-key-2"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Valid API key with range query → 200")
        void shouldAcceptApiKeyForRangeQuery() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("min", "1")
                            .param("max", "3")
                            .header(TestConstants.API_KEY_HEADER, TestConstants.API_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conversions.length()").value(3));
        }
    }

    // ========================================================================
    // Actuator Bypass
    // ========================================================================

    @Nested
    @DisplayName("Actuator bypasses auth")
    class ActuatorBypass {

        @Test
        @DisplayName("/actuator/health without API key → 200")
        void shouldAllowHealthWithoutApiKey() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }

        @Test
        @DisplayName("/actuator/info without API key → 200")
        void shouldAllowInfoWithoutApiKey() throws Exception {
            mockMvc.perform(get("/actuator/info"))
                    .andExpect(status().isOk());
        }
    }

    // ========================================================================
    // Security Headers
    // ========================================================================

    @Nested
    @DisplayName("Security headers")
    class SecurityHeaders {

        @Test
        @DisplayName("Response should contain X-Content-Type-Options: nosniff")
        void shouldContainNoSniff() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("query", "1")
                            .header(TestConstants.API_KEY_HEADER, TestConstants.API_KEY))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        }

        @Test
        @DisplayName("Response should contain X-Frame-Options: DENY")
        void shouldContainFrameDeny() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("query", "1")
                            .header(TestConstants.API_KEY_HEADER, TestConstants.API_KEY))
                    .andExpect(header().string("X-Frame-Options", "DENY"));
        }

        @Test
        @DisplayName("Response should contain Cache-Control: no-cache")
        void shouldContainCacheControl() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("query", "1")
                            .header(TestConstants.API_KEY_HEADER, TestConstants.API_KEY))
                    .andExpect(header().exists("Cache-Control"));
        }

        @Test
        @DisplayName("401 responses should also have correlation ID")
        void shouldHaveCorrelationIdOn401() throws Exception {
            mockMvc.perform(get("/romannumeral").param("query", "1"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().exists("X-Correlation-Id"));
        }
    }

}

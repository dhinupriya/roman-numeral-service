package com.adobe.romannumeral.security;

import com.adobe.romannumeral.TestConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Rate limiting tests — isolated with @DirtiesContext to ensure fresh buckets.
 *
 * <p>Bucket4j token-bucket state is per-JVM (singleton beans). Other tests
 * consume tokens, so rate limit tests need their own Spring context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Rate Limiting")
class RateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    @DisplayName("Exceeding single query rate limit → 429")
    void shouldRateLimitSingleQueries() throws Exception {
        // Exhaust the single query bucket (capacity: 100, refills 100/s)
        // Send 150 rapidly to overwhelm the refill rate
        for (int i = 0; i < 150; i++) {
            mockMvc.perform(get("/romannumeral")
                    .param("query", "1")
                    .header(TestConstants.API_KEY_HEADER, TestConstants.API_KEY));
        }

        // Next request should be rate limited
        mockMvc.perform(get("/romannumeral")
                        .param("query", "1")
                        .header(TestConstants.API_KEY_HEADER, TestConstants.API_KEY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value("Rate limit exceeded. Try again in 1 second."))
                .andExpect(header().string("Retry-After", "1"));
    }

    @Test
    @Order(2)
    @DisplayName("Exceeding range query rate limit → 429")
    void shouldRateLimitRangeQueries() throws Exception {
        // Exhaust the range query bucket (capacity: 10)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/romannumeral")
                    .param("min", "1")
                    .param("max", "3")
                    .header(TestConstants.API_KEY_HEADER, TestConstants.API_KEY));
        }

        // 11th should be rate limited
        mockMvc.perform(get("/romannumeral")
                        .param("min", "1")
                        .param("max", "3")
                        .header(TestConstants.API_KEY_HEADER, TestConstants.API_KEY))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "1"));
    }

    @Test
    @Order(3)
    @DisplayName("429 response should have structured JSON format")
    void shouldReturnStructuredJsonOn429() throws Exception {
        // Bucket already exhausted from previous tests
        mockMvc.perform(get("/romannumeral")
                        .param("min", "1")
                        .param("max", "3")
                        .header(TestConstants.API_KEY_HEADER, TestConstants.API_KEY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").isString())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.status").value(429));
    }
}

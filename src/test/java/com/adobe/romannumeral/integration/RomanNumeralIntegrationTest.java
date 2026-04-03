package com.adobe.romannumeral.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests — full Spring context, real beans, no mocks.
 *
 * <p>Verifies the complete request pipeline: controller → use case → converter → response,
 * including the CachedConverter for single queries and ChunkedParallelExecutor for range queries.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Integration Tests")
class RomanNumeralIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Single conversion E2E")
    class SingleConversionE2E {

        @Test
        @DisplayName("query=1 → I (uses CachedConverter)")
        void shouldConvertSingleNumber() throws Exception {
            mockMvc.perform(get("/romannumeral").param("query", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.input").value("1"))
                    .andExpect(jsonPath("$.output").value("I"));
        }

        @Test
        @DisplayName("query=1994 → MCMXCIV")
        void shouldConvertCompoundNumber() throws Exception {
            mockMvc.perform(get("/romannumeral").param("query", "1994"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.input").value("1994"))
                    .andExpect(jsonPath("$.output").value("MCMXCIV"));
        }

        @Test
        @DisplayName("query=3999 → MMMCMXCIX")
        void shouldConvertMaxValue() throws Exception {
            mockMvc.perform(get("/romannumeral").param("query", "3999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.output").value("MMMCMXCIX"));
        }
    }

    @Nested
    @DisplayName("Range conversion E2E")
    class RangeConversionE2E {

        @Test
        @DisplayName("min=1&max=3 → 3 ordered conversions")
        void shouldConvertSmallRange() throws Exception {
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
        @DisplayName("min=1&max=10 → 10 conversions in ascending order")
        void shouldConvertMediumRange() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("min", "1")
                            .param("max", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conversions.length()").value(10))
                    .andExpect(jsonPath("$.conversions[0].input").value("1"))
                    .andExpect(jsonPath("$.conversions[9].input").value("10"))
                    .andExpect(jsonPath("$.conversions[9].output").value("X"));
        }

        @Test
        @DisplayName("Full range 1-3999 → all conversions correct and in order")
        void shouldConvertFullRange() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("min", "1")
                            .param("max", "3999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conversions.length()").value(3999))
                    .andExpect(jsonPath("$.conversions[0].input").value("1"))
                    .andExpect(jsonPath("$.conversions[0].output").value("I"))
                    .andExpect(jsonPath("$.conversions[3998].input").value("3999"))
                    .andExpect(jsonPath("$.conversions[3998].output").value("MMMCMXCIX"))
                    // Spot-check middle values
                    .andExpect(jsonPath("$.conversions[1993].input").value("1994"))
                    .andExpect(jsonPath("$.conversions[1993].output").value("MCMXCIV"))
                    .andExpect(jsonPath("$.conversions[41].input").value("42"))
                    .andExpect(jsonPath("$.conversions[41].output").value("XLII"));
        }

        @Test
        @DisplayName("Range at upper boundary: 3998-3999")
        void shouldConvertBoundaryRange() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("min", "3998")
                            .param("max", "3999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conversions.length()").value(2))
                    .andExpect(jsonPath("$.conversions[0].input").value("3998"))
                    .andExpect(jsonPath("$.conversions[1].input").value("3999"));
        }
    }

    @Nested
    @DisplayName("Error handling E2E")
    class ErrorHandlingE2E {

        @Test
        @DisplayName("query=0 → 400")
        void shouldRejectZero() throws Exception {
            mockMvc.perform(get("/romannumeral").param("query", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("min=10&max=5 → 400 (reversed)")
        void shouldRejectReversedRange() throws Exception {
            mockMvc.perform(get("/romannumeral")
                            .param("min", "10")
                            .param("max", "5"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("No params → 400")
        void shouldRejectMissingParams() throws Exception {
            mockMvc.perform(get("/romannumeral"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Concurrent request safety")
    class ConcurrentRequestSafety {

        @Test
        @DisplayName("50 concurrent single requests should all succeed")
        void shouldHandleConcurrentSingleRequests() throws InterruptedException {
            int concurrency = 50;
            AtomicInteger errors = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(concurrency);

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 0; i < concurrency; i++) {
                    final int number = (i % 3999) + 1;
                    executor.submit(() -> {
                        try {
                            mockMvc.perform(get("/romannumeral")
                                            .param("query", String.valueOf(number)))
                                    .andExpect(status().isOk())
                                    .andExpect(jsonPath("$.input").value(String.valueOf(number)));
                        } catch (Exception e) {
                            errors.incrementAndGet();
                        } finally {
                            latch.countDown();
                        }
                    });
                }
                latch.await();
            }
            org.assertj.core.api.Assertions.assertThat(errors.get()).isZero();
        }

        @Test
        @DisplayName("10 concurrent range requests should all succeed with correct ordering")
        void shouldHandleConcurrentRangeRequests() throws InterruptedException {
            int concurrency = 10;
            AtomicInteger errors = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(concurrency);

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 0; i < concurrency; i++) {
                    executor.submit(() -> {
                        try {
                            mockMvc.perform(get("/romannumeral")
                                            .param("min", "1")
                                            .param("max", "100"))
                                    .andExpect(status().isOk())
                                    .andExpect(jsonPath("$.conversions.length()").value(100))
                                    .andExpect(jsonPath("$.conversions[0].input").value("1"))
                                    .andExpect(jsonPath("$.conversions[99].input").value("100"));
                        } catch (Exception e) {
                            errors.incrementAndGet();
                        } finally {
                            latch.countDown();
                        }
                    });
                }
                latch.await();
            }
            org.assertj.core.api.Assertions.assertThat(errors.get()).isZero();
        }
    }
}

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
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Rate limiting tests — isolated with @DirtiesContext to ensure fresh buckets.
 *
 * <p>Uses parallel requests to overwhelm the greedy refill rate.
 * Sequential requests can't outpace the refill (100 tokens/second refill
 * faster than MockMvc request overhead), so we fire requests concurrently.
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
    @DisplayName("Exceeding single query rate limit → at least one 429 among burst")
    void shouldRateLimitSingleQueries() throws InterruptedException {
        // Fire 200 requests concurrently — bucket capacity is 100 with greedy refill 100/s.
        // Some must get 429 since burst exceeds capacity.
        int totalRequests = 200;
        AtomicInteger status429Count = new AtomicInteger(0);
        AtomicInteger status200Count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < totalRequests; i++) {
                executor.submit(() -> {
                    try {
                        MvcResult result = mockMvc.perform(get("/romannumeral")
                                        .param("query", "1")
                                        .header(TestConstants.API_KEY_HEADER, TestConstants.API_KEY))
                                .andReturn();
                        if (result.getResponse().getStatus() == 429) {
                            status429Count.incrementAndGet();
                        } else if (result.getResponse().getStatus() == 200) {
                            status200Count.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // ignore
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }

        // At least some requests must be rate-limited
        assertThat(status429Count.get())
                .as("Expected some 429 responses among %d requests (got %d×200, %d×429)",
                        totalRequests, status200Count.get(), status429Count.get())
                .isGreaterThan(0);
    }

    @Test
    @Order(2)
    @DisplayName("Exceeding range query rate limit → at least one 429 among burst")
    void shouldRateLimitRangeQueries() throws InterruptedException {
        // Fire 30 range requests concurrently — bucket capacity is 10
        int totalRequests = 30;
        AtomicInteger status429Count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < totalRequests; i++) {
                executor.submit(() -> {
                    try {
                        MvcResult result = mockMvc.perform(get("/romannumeral")
                                        .param("min", "1")
                                        .param("max", "3")
                                        .header(TestConstants.API_KEY_HEADER, TestConstants.API_KEY))
                                .andReturn();
                        if (result.getResponse().getStatus() == 429) {
                            status429Count.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // ignore
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }

        assertThat(status429Count.get())
                .as("Expected some 429 responses for range queries")
                .isGreaterThan(0);
    }

    @Test
    @Order(3)
    @DisplayName("429 response should have structured JSON and Retry-After header")
    void shouldReturnStructuredJsonOn429() throws Exception {
        // Exhaust range bucket with concurrent burst
        int totalRequests = 30;
        CountDownLatch latch = new CountDownLatch(totalRequests);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < totalRequests; i++) {
                executor.submit(() -> {
                    try {
                        mockMvc.perform(get("/romannumeral")
                                .param("min", "1")
                                .param("max", "3")
                                .header(TestConstants.API_KEY_HEADER, TestConstants.API_KEY));
                    } catch (Exception e) {
                        // ignore
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }

        // Send one more — likely to be rate-limited after burst
        // Try a few times to catch a 429
        boolean got429 = false;
        for (int i = 0; i < 20; i++) {
            MvcResult result = mockMvc.perform(get("/romannumeral")
                            .param("min", "1")
                            .param("max", "3")
                            .header(TestConstants.API_KEY_HEADER, TestConstants.API_KEY))
                    .andReturn();
            if (result.getResponse().getStatus() == 429) {
                // Verify the 429 response format
                mockMvc.perform(get("/romannumeral")
                                .param("min", "1")
                                .param("max", "3")
                                .header(TestConstants.API_KEY_HEADER, TestConstants.API_KEY))
                        .andExpect(status().isTooManyRequests())
                        .andExpect(jsonPath("$.error").value("Too Many Requests"))
                        .andExpect(jsonPath("$.status").value(429))
                        .andExpect(header().string("Retry-After", "1"));
                got429 = true;
                break;
            }
        }
        assertThat(got429).as("Should have received at least one 429 response").isTrue();
    }
}

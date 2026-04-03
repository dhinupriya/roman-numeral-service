package com.adobe.romannumeral.infrastructure;

import com.adobe.romannumeral.domain.model.RomanNumeralResult;
import com.adobe.romannumeral.infrastructure.execution.ChunkedParallelExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ChunkedParallelExecutor — verifies chunking, ordering, and edge cases.
 */
@DisplayName("ChunkedParallelExecutor")
class ChunkedParallelExecutorTest {

    @Nested
    @DisplayName("Result ordering")
    class ResultOrdering {

        @Test
        @DisplayName("Results should be in ascending order for range 1-100")
        void shouldPreserveOrderForMediumRange() {
            var executor = new ChunkedParallelExecutor();
            List<RomanNumeralResult> results = executor.executeRange(1, 100,
                    n -> new RomanNumeralResult(String.valueOf(n), "R" + n));

            assertThat(results).hasSize(100);
            for (int i = 0; i < results.size(); i++) {
                assertThat(results.get(i).input()).isEqualTo(String.valueOf(i + 1));
            }
        }

        @Test
        @DisplayName("Results should be in ascending order for full range 1-3999")
        void shouldPreserveOrderForFullRange() {
            var executor = new ChunkedParallelExecutor();
            List<RomanNumeralResult> results = executor.executeRange(1, 3999,
                    n -> new RomanNumeralResult(String.valueOf(n), "R" + n));

            assertThat(results).hasSize(3999);
            for (int i = 0; i < results.size(); i++) {
                assertThat(results.get(i).input())
                        .as("Position %d should be %d", i, i + 1)
                        .isEqualTo(String.valueOf(i + 1));
            }
        }
    }

    @Nested
    @DisplayName("Small ranges (fewer items than cores)")
    class SmallRanges {

        @Test
        @DisplayName("Range of 2 should work (sequential fallback)")
        void shouldHandleRangeOfTwo() {
            var executor = new ChunkedParallelExecutor();
            List<RomanNumeralResult> results = executor.executeRange(1, 2,
                    n -> new RomanNumeralResult(String.valueOf(n), "R" + n));

            assertThat(results).hasSize(2);
            assertThat(results.get(0).input()).isEqualTo("1");
            assertThat(results.get(1).input()).isEqualTo("2");
        }

        @Test
        @DisplayName("Range of 1 should work (single element)")
        void shouldHandleRangeOfOne() {
            var executor = new ChunkedParallelExecutor();
            List<RomanNumeralResult> results = executor.executeRange(5, 5,
                    n -> new RomanNumeralResult(String.valueOf(n), "R" + n));

            assertThat(results).hasSize(1);
            assertThat(results.get(0).input()).isEqualTo("5");
        }
    }

    @Nested
    @DisplayName("Chunking behavior")
    class ChunkingBehavior {

        @Test
        @DisplayName("With parallelism=2, range 1-10 should produce 10 ordered results")
        void shouldChunkCorrectlyWithTwoThreads() {
            var executor = new ChunkedParallelExecutor(2);
            List<RomanNumeralResult> results = executor.executeRange(1, 10,
                    n -> new RomanNumeralResult(String.valueOf(n), "R" + n));

            assertThat(results).hasSize(10);
            for (int i = 0; i < 10; i++) {
                assertThat(results.get(i).input()).isEqualTo(String.valueOf(i + 1));
            }
        }

        @Test
        @DisplayName("With parallelism=1, should still produce correct ordered results")
        void shouldWorkWithSingleThread() {
            var executor = new ChunkedParallelExecutor(1);
            List<RomanNumeralResult> results = executor.executeRange(1, 20,
                    n -> new RomanNumeralResult(String.valueOf(n), "R" + n));

            assertThat(results).hasSize(20);
            for (int i = 0; i < 20; i++) {
                assertThat(results.get(i).input()).isEqualTo(String.valueOf(i + 1));
            }
        }

        @Test
        @DisplayName("Remainder distribution — 10 items across 3 threads (4+3+3)")
        void shouldDistributeRemainderEvenly() {
            var executor = new ChunkedParallelExecutor(3);
            List<RomanNumeralResult> results = executor.executeRange(1, 10,
                    n -> new RomanNumeralResult(String.valueOf(n), "R" + n));

            assertThat(results).hasSize(10);
            for (int i = 0; i < 10; i++) {
                assertThat(results.get(i).input()).isEqualTo(String.valueOf(i + 1));
            }
        }
    }

    @Nested
    @DisplayName("Non-zero start range")
    class NonZeroStart {

        @Test
        @DisplayName("Range 500-510 should produce 11 ordered results starting at 500")
        void shouldHandleNonZeroStart() {
            var executor = new ChunkedParallelExecutor();
            List<RomanNumeralResult> results = executor.executeRange(500, 510,
                    n -> new RomanNumeralResult(String.valueOf(n), "R" + n));

            assertThat(results).hasSize(11);
            assertThat(results.get(0).input()).isEqualTo("500");
            assertThat(results.get(10).input()).isEqualTo("510");
        }
    }

    @Nested
    @DisplayName("Concurrent safety")
    class ConcurrentSafety {

        @Test
        @DisplayName("Multiple concurrent executeRange calls should all return correct results")
        void shouldBeThreadSafe() throws InterruptedException {
            var executor = new ChunkedParallelExecutor();
            var errors = new java.util.concurrent.atomic.AtomicInteger(0);

            Thread[] threads = new Thread[10];
            for (int t = 0; t < threads.length; t++) {
                final int offset = t * 100;
                threads[t] = Thread.startVirtualThread(() -> {
                    List<RomanNumeralResult> results = executor.executeRange(
                            offset + 1, offset + 100,
                            n -> new RomanNumeralResult(String.valueOf(n), "R" + n));
                    if (results.size() != 100) errors.incrementAndGet();
                    for (int i = 0; i < results.size(); i++) {
                        if (!results.get(i).input().equals(String.valueOf(offset + i + 1))) {
                            errors.incrementAndGet();
                        }
                    }
                });
            }
            for (Thread t : threads) t.join(5000);
            assertThat(errors.get()).isZero();
        }
    }
}

package com.adobe.romannumeral.infrastructure.execution;

import com.adobe.romannumeral.application.port.ParallelExecutionPort;
import com.adobe.romannumeral.domain.model.RomanNumeralResult;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Chunked parallel executor — splits a range into CPU-core-sized chunks and
 * executes each chunk on a virtual thread.
 *
 * <p><b>Why chunked, not per-number?</b>
 * Each {@code convert()} call takes ~1μs. Thread creation takes ~10-50μs.
 * With 3999 threads, you'd spend 50x more time managing threads than doing work.
 * With 8 threads (1 per CPU core), each thread does ~500 sequential conversions
 * — optimal CPU utilization with minimal overhead.
 *
 * <p><b>Why not parallel streams?</b>
 * {@code ForkJoinPool.commonPool()} is shared across the JVM. A large range query
 * would starve Tomcat's request handling threads. Dedicated executor gives isolation.
 *
 * <p><b>How ordering is preserved:</b>
 * Futures are created in chunk order: [chunk1, chunk2, ..., chunk8].
 * Within each chunk, numbers are processed sequentially in ascending order.
 * Joining futures in creation order produces ascending results automatically
 * — no sorting needed (O(n) assembly, not O(n log n)).
 *
 * <p><b>Thread safety:</b> This class is stateless. The executor is created per-call
 * (virtual thread executors are lightweight). All state lives in method-local variables.
 */
@Slf4j
public class ChunkedParallelExecutor implements ParallelExecutionPort {

    private final int parallelism;

    /**
     * Creates an executor with parallelism matching available CPU cores.
     */
    public ChunkedParallelExecutor() {
        this(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Creates an executor with specified parallelism level.
     * Visible for testing — allows controlling the number of chunks.
     */
    public ChunkedParallelExecutor(int parallelism) {
        this.parallelism = Math.max(1, parallelism);
    }

    @Override
    public List<RomanNumeralResult> executeRange(
            int startInclusive,
            int endInclusive,
            Function<Integer, RomanNumeralResult> converter) {

        int totalSize = endInclusive - startInclusive + 1;

        // For very small ranges, don't bother with parallelism
        if (totalSize <= parallelism) {
            return executeSequentially(startInclusive, endInclusive, converter);
        }

        int chunkCount = Math.min(parallelism, totalSize);
        int chunkSize = totalSize / chunkCount;
        int remainder = totalSize % chunkCount;

        log.debug("Executing range [{}-{}] with {} chunks of ~{} each on virtual threads",
                startInclusive, endInclusive, chunkCount, chunkSize);

        List<CompletableFuture<List<RomanNumeralResult>>> futures = new ArrayList<>(chunkCount);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            int chunkStart = startInclusive;

            for (int i = 0; i < chunkCount; i++) {
                // Distribute remainder across first few chunks (1 extra each)
                int currentChunkSize = chunkSize + (i < remainder ? 1 : 0);
                int chunkEnd = chunkStart + currentChunkSize - 1;

                final int start = chunkStart;
                final int end = chunkEnd;

                futures.add(CompletableFuture.supplyAsync(
                        () -> executeSequentially(start, end, converter),
                        executor));

                chunkStart = chunkEnd + 1;
            }

            // Join futures in creation order — preserves ascending order without sorting
            return futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .toList();
        }
    }

    /**
     * Sequentially converts a sub-range. Each virtual thread runs this for its chunk.
     */
    private List<RomanNumeralResult> executeSequentially(
            int startInclusive,
            int endInclusive,
            Function<Integer, RomanNumeralResult> converter) {

        List<RomanNumeralResult> results = new ArrayList<>(endInclusive - startInclusive + 1);
        for (int i = startInclusive; i <= endInclusive; i++) {
            results.add(converter.apply(i));
        }
        return results;
    }
}

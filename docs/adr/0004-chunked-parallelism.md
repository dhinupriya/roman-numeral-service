# ADR-0004: Chunked Parallelism (CPU-Core-Based, Not Per-Number)

## Status
Accepted

## Context
Range queries require parallel computation. Need to decide the granularity of parallelism: one thread per number (3999 threads for max range) vs. chunked by CPU cores (~8 threads).

## Options Considered
1. **Thread per number** — 3999 virtual threads, each converts one number. Thread creation (~10-50μs) exceeds computation (~1μs) by 10-50x. More time managing threads than doing work.
2. **Chunked by CPU cores** — 8 virtual threads (1 per core), each converts ~500 numbers sequentially. Thread creation (8 × ~50μs = 400μs) is negligible compared to work (500μs per chunk). Optimal CPU utilization.
3. **Parallel streams** — uses shared `ForkJoinPool.commonPool()`. A large range query would starve Tomcat's request handling threads. No isolation.
4. **Nested parallelism** — parallel chunks + parallel within chunks. Adds scheduling overhead with zero gain since work is already distributed across all CPU cores.

## Decision
Chunked by CPU cores (Option 2) via `ChunkedParallelExecutor` using `CompletableFuture.supplyAsync()` with `newVirtualThreadPerTaskExecutor()`.

## Consequences
- **Gain:** 8 threads vs. 3999 — minimal overhead, optimal CPU utilization. Order preserved without sorting (futures joined in creation order). Dedicated executor gives isolation from Tomcat threads.
- **Trade-off:** Small ranges (< CPU cores) fall back to sequential — handled by a size check in the executor.
- **Key insight:** A principal engineer matches parallelism to the cost of the work unit, not the count of work items.
- **Remainder handling:** When range doesn't divide evenly by core count, first N chunks get 1 extra item each for even distribution.

# ADR-0003: Dual Converter Strategy (Cached + Standard)

## Status
Accepted

## Context
The assessment requires: "Use multithreading to compute the values in the range in parallel." Single queries should also be fast. Need to satisfy the multithreading requirement while optimizing single query performance.

## Options Considered
1. **No cache, always compute** — every request runs the algorithm. Range uses parallel threads. Simple but single queries recompute every time (~1μs, negligible but suboptimal).
2. **Precomputed cache for everything** — build cache at startup. Single + range both read from cache. Fastest, but range query isn't parallel at request time — just array reads. Reviewer may question: "Where's the multithreading?"
3. **Dual converter** — CachedConverter for single queries (O(1) lookup), StandardConverter + parallel for range queries (real computation).
4. **Parallel cache build, cache read for range** — parallelism at startup, not at request time. Strict reading of requirement: "compute values in the range in parallel" implies request-time computation.

## Decision
Dual converter (Option 3). CachedRomanNumeralConverter wraps StandardRomanNumeralConverter (Decorator pattern). Single queries use cache. Range queries use the raw algorithm with chunked parallel execution.

## Consequences
- **Gain:** Directly satisfies the multithreading requirement — undeniable parallel computation at request time. Single queries optimized to O(1). Shows architectural depth (Strategy + Decorator + Port/Adapter patterns).
- **Trade-off:** Two converter implementations to maintain. Slightly more complex bean wiring (solved with explicit ConverterConfig).
- **Enables:** Explicit ConverterConfig for bean wiring — all wiring decisions in one file, no @Primary/@Qualifier annotation hunting.

# ADR-0001: Clean/Hexagonal Architecture over Layered

## Status
Accepted

## Context
The assessment requires a production-grade HTTP service demonstrating senior engineering quality. We need an architecture that separates concerns, enables testability, and allows future extension without modifying core business logic.

## Options Considered
1. **Simple Layered Architecture** (Controller → Service → Repository) — straightforward, widely understood. But dependencies flow downward — business logic depends on infrastructure. Testing the converter requires Spring context.
2. **Clean/Hexagonal Architecture** — dependencies point inward. Domain has zero framework imports. Infrastructure adapts to domain interfaces (ports). Testing is plain Java — `new StandardRomanNumeralConverter()`.
3. **Vertical Slice Architecture** — organized by feature, not layer. Overkill for a single-feature service.

## Decision
Clean/Hexagonal Architecture. Domain layer defines interfaces (ports), infrastructure provides implementations (adapters). Domain has zero Spring imports.

## Consequences
- **Gain:** Converter testable without Spring context. Threading strategy swappable via ParallelExecutionPort. Can add CLI/gRPC adapters without touching domain. Framework upgrades don't affect business logic.
- **Trade-off:** More packages and indirection than simple layered. New developers need to understand the pattern.
- **Enables:** Strategy pattern for dual converters, Port/Adapter for parallel execution, Decorator for caching — all naturally fit hexagonal boundaries.

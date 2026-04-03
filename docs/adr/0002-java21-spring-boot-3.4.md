# ADR-0002: Java 21 + Spring Boot 3.4.1

## Status
Accepted

## Context
Need to choose a Java version and Spring Boot version that supports virtual threads for parallel range computation, records for Value Objects, sealed classes for exception hierarchy, and has a mature ecosystem.

## Options Considered
1. **Java 17 + Spring Boot 3.2.x** — LTS, stable, but no virtual threads (requires ThreadPoolTaskExecutor with manual pool sizing).
2. **Java 21 + Spring Boot 3.4.1** — LTS (support until 2031+), virtual threads, records, sealed classes, pattern matching. Spring Boot 3.4.x has first-class Java 21 support. 16+ months in production.
3. **Java 25 + Spring Boot 3.5+** — Latest LTS, but only ~6 months old. Ecosystem compatibility not guaranteed (Micrometer, Logstash encoder, Lombok).

## Decision
Java 21 + Spring Boot 3.4.1. Sweet spot of modern features with mature ecosystem.

## Consequences
- **Gain:** Virtual threads eliminate thread pool configuration. Records for immutable VOs. Sealed classes for exhaustive exception handling. Spring Boot 3.4 has native structured logging and enhanced Micrometer.
- **Trade-off:** Not the absolute latest (Java 25), but zero risk of compatibility issues.
- **Note:** Spring Boot 3.4 requires `management.prometheus.metrics.export.enabled=true` for the Prometheus actuator endpoint — discovered during Phase 3 testing.

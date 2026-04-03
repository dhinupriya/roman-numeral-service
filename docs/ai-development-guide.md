# AI Development Guide — Roman Numeral Conversion Service

> **Auto-sync:** This is the single source of truth for AI-assisted development.
> Run `./scripts/sync-ai-conventions.sh all` to distribute to all AI tool files.

## Project Overview

Production-grade HTTP service converting integers (1-3999) to Roman numerals.
Java 21, Spring Boot 3.4.1, Clean/Hexagonal Architecture, Maven.

- Single conversion: `GET /romannumeral?query={integer}` → `{"input":"1","output":"I"}`
- Range conversion: `GET /romannumeral?min={int}&max={int}` → `{"conversions":[...]}`
- API key required: `X-API-Key` header

## Architecture Rules — MUST Follow

### Clean/Hexagonal Architecture
- **Domain layer has ZERO Spring imports** — no `@Component`, `@Service`, `@Autowired`
- **Dependencies point inward only** — domain knows nothing about infrastructure
- Domain defines interfaces (ports), infrastructure provides implementations (adapters)
- Test domain with `new StandardRomanNumeralConverter()` — no Spring context needed

### Package Structure
```
domain/          → models, interfaces, exceptions (zero framework deps)
application/     → use cases, ports (orchestration only)
infrastructure/  → converters, config, security, observability (Spring lives here)
web/             → controller, DTOs, error handler (HTTP concerns only)
```

### Bean Wiring
- **Use `ConverterConfig` for explicit wiring** — all wiring in one file
- **NEVER use `@Primary` or `@Qualifier`** — implicit, hard to debug
- **NEVER add `@Component`/`@Service` to domain or application classes**
- Only infrastructure and web classes get Spring annotations

## Code Conventions

### Value Objects
- Use Java **records** — immutable, self-validating, thread-safe
- Validate in compact constructor (fail-fast)
- Example: `RomanNumeralRange` validates `min < max` at construction

### Exceptions
- **Sealed hierarchy**: `DomainException` (sealed) → `InvalidInputException`, `InvalidRangeException`
- Each maps to exactly one HTTP status (400)
- `GlobalExceptionHandler` handles ALL exceptions — no Whitelabel, no stack traces exposed

### Controller
- Accept **String parameters** (not int) — we control parsing error messages
- Sanitize user input in error messages (XSS prevention)
- Truncate long inputs (> 50 chars)
- Precedence: `query` param wins over `min`/`max`

### Constructor Injection
- `@RequiredArgsConstructor` everywhere — never field injection
- Fields are `final` — immutable after construction

### DTOs
- Use `Response.from()` factory methods — mapping co-located with DTO
- JSON field names match assessment spec exactly: `input`, `output`, `conversions`

## Parallelism Rules

### Converter Strategy
- **Single queries** → `CachedRomanNumeralConverter` (O(1) array lookup)
- **Range queries** → `StandardRomanNumeralConverter` + `ChunkedParallelExecutor`
- NEVER use cached converter for range queries — assessment requires real parallel computation

### Threading
- **ChunkedParallelExecutor**: 1 virtual thread per CPU core, NOT per number
- **NEVER use parallel streams** — shared ForkJoinPool, no isolation
- **NEVER create 3999 threads** — thread creation cost > computation cost
- Order preserved by joining CompletableFutures in creation order — no sorting

## Security Patterns

### Filter Ordering (Critical)
```
CorrelationIdFilter (HIGHEST_PRECEDENCE)     → UUID on all responses
RequestLoggingFilter (HIGHEST_PRECEDENCE+1)  → log all requests including rejects
ApiKeyAuthFilter (HIGHEST_PRECEDENCE+2)      → 401 before rate limit
RateLimitFilter (HIGHEST_PRECEDENCE+3)       → 429, don't consume tokens for unauth
SecurityFilterChain                          → headers, CORS
```

### Error Responses from Filters
- Use `FilterResponseHelper.writeErrorResponse()` — consistent JSON format
- Filters write responses directly — `GlobalExceptionHandler` doesn't see them
- Format: `{"error":"...","message":"...","status":N}`

### API Key
- Stored in `application.yml` (production: secrets manager)
- Actuator + Swagger skip auth (`shouldNotFilter`)

## Observability Patterns

### Metrics
- `ConversionMetrics` is a **collaborator** injected into use cases — not AOP, not decorator
- Errors recorded in `GlobalExceptionHandler` — single place all errors flow through
- Timers use `.publishPercentileHistogram(true)` for p50/p95/p99 in Grafana

### Logging
- `CorrelationIdFilter` adds UUID to MDC — appears in all log lines
- `RequestLoggingFilter` adds: clientIp, method, uri, status, durationMs, responseSize to MDC
- Skips `/actuator` endpoints (would flood logs from Prometheus scraping)
- MDC cleanup in `finally` block — critical for virtual thread reuse

### Health
- Custom `ConversionHealthIndicator`: `convert(1) == "I"`
- Uses `CachedRomanNumeralConverter` — if cache is healthy, single queries work

## Testing Rules

### Unit Tests
- Plain Java, no Spring context: `new StandardRomanNumeralConverter()`
- Parameterized with `@CsvSource` for converter tests
- Mock `ConversionMetrics` in use case tests

### @WebMvcTest (Controller Slice)
- Import `GlobalExceptionHandler` and `SecurityConfig`
- **Exclude `ApiKeyAuthFilter` and `RateLimitFilter`** — testing routing, not security
- Mock use cases and `AppProperties`

### @SpringBootTest (Integration)
- Include `X-API-Key` header via `TestConstants.API_KEY`
- Use `@DirtiesContext` for tests with shared stateful beans (rate limiter)
- Actuator tests use `TestRestTemplate` with `RANDOM_PORT` (not MockMvc)

### Test Constants
- `TestConstants.API_KEY` and `TestConstants.API_KEY_HEADER` — no magic strings

## DO NOT

- Don't add Spring annotations to domain classes
- Don't use `@Primary` or `@Qualifier` — use `ConverterConfig`
- Don't use parallel streams — use `ChunkedParallelExecutor`
- Don't use `HashMap` for cache — use array (contiguous integer keys)
- Don't expose stack traces in error responses
- Don't log actuator requests (floods logs)
- Don't skip MDC cleanup (virtual thread leak)
- Don't hardcode API keys — use `application.yml` or environment variables
- Don't use `@Autowired` field injection — use constructor injection
- Don't put business logic in controllers — use cases handle orchestration

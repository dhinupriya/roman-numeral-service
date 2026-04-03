# ADR-0007: API Key Authentication + Bucket4j Rate Limiting

## Status
Accepted

## Context
Need to protect the API from unauthorized access and abuse. The assessment doesn't require security, but production-grade services need access control and rate limiting as defense-in-depth.

## Options Considered

### Authentication
1. **No auth** — open API. Simple but no access control.
2. **API Key (X-API-Key header)** — simple header check. Appropriate for service-to-service and assessments.
3. **OAuth2/JWT** — full token lifecycle (issuance, validation, refresh, scopes). Overkill for a single-purpose API — belongs at API Gateway layer.

### Rate Limiting
1. **Global bucket** — one limit for all requests. A flood of range queries starves single queries.
2. **Per-endpoint buckets** — separate limits: 100 req/s single, 10 req/s range. Fair — different costs get different limits.
3. **Per-client (IP-based)** — fair per user but proxies complicate client identification. X-Forwarded-For is spoofable.

### Implementation
1. **bucket4j-spring-boot-starter** — auto-configured via properties. Convenient but opaque.
2. **bucket4j-core + custom filter** — explicit code, testable, no starter magic. Consistent with our explicit-over-implicit philosophy (ConverterConfig).

## Decision
- API Key authentication via custom servlet filter (ApiKeyAuthFilter)
- Bucket4j-core with custom RateLimitFilter — per-endpoint buckets (100/s single, 10/s range)
- Keys stored in application.yml (documented: production should use secrets manager)
- OAuth2/JWT documented as Tier 3 production recommendation

## Consequences
- **Gain:** Access control + abuse protection with minimal complexity. Per-endpoint limits prevent expensive range queries from starving single queries. Structured JSON errors (401, 429) consistent with all other errors.
- **Trade-off:** API keys in config file — acceptable for assessment, not for production. Documented in code and README.
- **Filter ordering:** Correlation → Logging → Auth → Rate Limit. Auth before rate limit ensures unauthenticated requests don't consume tokens. Correlation ID on all responses (including 401/429) for debugging.

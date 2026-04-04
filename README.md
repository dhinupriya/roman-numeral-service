# Roman Numeral Conversion Service

[![CI](https://github.com/dhinupriya/roman-numeral-service/actions/workflows/ci.yml/badge.svg)](https://github.com/dhinupriya/roman-numeral-service/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://docs.oracle.com/en/java/javase/21/)
[![Spring Boot 3.4.1](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen?logo=springboot)](https://docs.spring.io/spring-boot/3.4/)
[![Tests](https://img.shields.io/badge/Tests-192%20passing-brightgreen?logo=junit5)](https://github.com/dhinupriya/roman-numeral-service/actions/workflows/ci.yml)
[![Coverage](https://img.shields.io/badge/Coverage-94.9%25-brightgreen?logo=jacoco)](https://github.com/dhinupriya/roman-numeral-service/actions/workflows/ci.yml)

Production-grade HTTP service that converts integers (1-3999) to Roman numerals. Built with Java 21, Spring Boot 3.4, and Clean Architecture. Supports single conversions and parallel range queries using chunked virtual thread execution.

**Roman Numeral Specification**: [Wikipedia: Roman Numerals — Standard Form](https://en.wikipedia.org/wiki/Roman_numerals#Standard_form)

---

## Table of Contents

- [Step-by-Step Walkthrough](#step-by-step-walkthrough)
- [API Documentation](#api-documentation)
- [Architecture](#architecture)
- [Engineering Methodology](#engineering-methodology)
- [Testing Methodology](#testing-methodology)
- [Packaging Layout](#packaging-layout)
- [Observability](#observability)
- [Security](#security)
- [Load Testing](#load-testing)
- [Docker](#docker)
- [CI/CD](#cicd)
- [AI Integration](#ai-integration)
- [Architecture Decision Records](#architecture-decision-records)
- [Dependency Attribution](#dependency-attribution)
- [Production Roadmap](#production-roadmap)

---

## Step-by-Step Walkthrough

**Prerequisites:** Java 21+, Maven 3.9+ (or use `./mvnw`), Docker & Docker Compose, Python 3.11+ (optional, for AI tools)

Each step builds on the previous one.

### Step 1: Build & Test (2 minutes)
```bash
git clone https://github.com/dhinupriya/roman-numeral-service.git
cd roman-numeral-service
./mvnw clean verify
```
Expected: **192 tests passing**, JaCoCo coverage ≥ 80%, **BUILD SUCCESS**

### Step 2: Run Locally & Test API (1 minute)
```bash
./mvnw spring-boot:run
```
In another terminal:
```bash
# Single conversion
curl -H "X-API-Key: test-api-key-1" "localhost:8080/romannumeral?query=1994"
# → {"input":"1994","output":"MCMXCIV"}

# Range conversion (parallel)
curl -H "X-API-Key: test-api-key-1" "localhost:8080/romannumeral?min=1&max=10"
# → {"conversions":[{"input":"1","output":"I"},...{"input":"10","output":"X"}]}

# Full range (3999 values, parallel chunked computation)
curl -H "X-API-Key: test-api-key-1" "localhost:8080/romannumeral?min=1&max=3999" | head -c 200

# Error handling
curl "localhost:8080/romannumeral?query=1"           # → 401 (no API key)
curl -H "X-API-Key: test-api-key-1" "localhost:8080/romannumeral?query=0"    # → 400
curl -H "X-API-Key: test-api-key-1" "localhost:8080/romannumeral?min=10&max=5"  # → 400
```
Stop the app with Ctrl+C.

### Step 3: Swagger UI (30 seconds)
Start the app again, then open: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

Click "Authorize" → enter `test-api-key-1` → "Try it out" on the endpoint.

### Step 4: Health & Metrics (30 seconds)
```bash
# Health check (no API key needed)
curl -s "localhost:8080/actuator/health" | python3 -m json.tool

# Prometheus metrics
curl -s "localhost:8080/actuator/prometheus" | grep roman_conversion
```
Stop the app with Ctrl+C.

### Step 5: Docker Compose — Full Observability Stack (3 minutes)
```bash
docker compose up -d
docker compose ps   # wait until all show "healthy" or "Up" (~45 seconds)
```
```bash
# Test the Dockerized app
curl -H "X-API-Key: test-api-key-1" "localhost:8080/romannumeral?query=42"

# Generate traffic for dashboards
for i in $(seq 1 50); do curl -s -H "X-API-Key: test-api-key-1" "localhost:8080/romannumeral?query=$((RANDOM % 3999 + 1))" > /dev/null; done
curl -s -H "X-API-Key: test-api-key-1" "localhost:8080/romannumeral?min=1&max=3999" > /dev/null
curl -s -H "X-API-Key: test-api-key-1" "localhost:8080/romannumeral?query=0" > /dev/null
curl -s "localhost:8080/romannumeral?query=1" > /dev/null
```

### Step 6: Grafana Dashboards (2 minutes)
Open [http://localhost:3000](http://localhost:3000) (login: `admin` / `admin`, skip password change)

Navigate to **Roman Numeral Service** folder:
- **Application Dashboard** → conversion rates, latency p50/p95/p99, errors, JVM
- **Infrastructure Dashboard** → container CPU, memory, host stats
- **Logs Dashboard** → log volume, error/warn logs, 401s, 429s

### Step 7: k6 Load Test (5 minutes)
```bash
# Run load test (app must be running via docker compose)
docker compose -f k6/docker-compose.k6.yml run --rm k6-load
```
Watch the Grafana App Dashboard in real-time during the test — metrics update live.

### Step 8: Graceful Shutdown
```bash
docker compose down
```
All containers stop cleanly.

### Step 9: AI Integration (optional, requires Python 3.11+ and an LLM API key)

**AI Development Guide:**
```bash
# Generate conventions for your AI tool
./scripts/sync-ai-conventions.sh claude    # if you use Claude Code
./scripts/sync-ai-conventions.sh cursor    # if you use Cursor
./scripts/sync-ai-conventions.sh copilot   # if you use GitHub Copilot
./scripts/sync-ai-conventions.sh gemini    # if you use Gemini
./scripts/sync-ai-conventions.sh windsurf  # if you use Windsurf
./scripts/sync-ai-conventions.sh all       # generate all at once
```

**AI Code Review Agent:**
```bash
# Setup (one-time)
cd scripts && python3 -m venv venv && source venv/bin/activate && pip install -r requirements.txt && cd ..

# Set your API key
export ANTHROPIC_API_KEY=sk-ant-your-key

# Dry run (see what would be reviewed, no API call)
python3 scripts/ai-review.py --all --dry-run

# Review entire project (~$0.19 with Sonnet)
python3 scripts/ai-review.py --all --model claude-sonnet-4-20250514

# Review a single file (~$0.03)
python3 scripts/ai-review.py src/main/java/com/adobe/romannumeral/web/controller/RomanNumeralController.java --model claude-sonnet-4-20250514
```
> **Tested with:** Anthropic Claude Sonnet 4 (`claude-sonnet-4-20250514`). Also supports OpenAI (`--model gpt-4o`, requires `OPENAI_API_KEY`) and Google Gemini (`--model gemini-1.5-pro`, requires `GOOGLE_API_KEY`) — these providers are implemented but not tested.

**MCP Server (connect AI agents to the service):**
```bash
# 1. Install MCP server dependencies (one-time setup)
cd mcp-server && python3 -m venv venv && source venv/bin/activate && pip install -r requirements.txt && cd ..

# 2. Make sure the Roman numeral service is running (via mvnw or docker compose)

# 3. Register MCP server with Claude Code (run from project root, use full absolute paths)
claude mcp add roman-numeral \
  -e SERVICE_URL=http://localhost:8080 \
  -e API_KEY=test-api-key-1 \
  -- /absolute/path/to/roman-numeral-service/mcp-server/venv/bin/python3 \
  /absolute/path/to/roman-numeral-service/mcp-server/server.py

# 4. Start a new Claude Code session from the project root
claude
```
Then ask: *"Convert 1994 to a Roman numeral"*

> **Important:** Run `claude mcp add` from the project root (not `mcp-server/`) so the MCP config is scoped correctly. Use full absolute paths for both the venv python and server.py.

For Cursor, VS Code, Claude Desktop setup: see [`mcp-server/README.md`](mcp-server/README.md)

---

## API Documentation

### Interactive Swagger UI

Available at [localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) (no API key required to view). Click "Authorize" → enter `test-api-key-1` to execute requests via "Try it out".

![Swagger UI](docs/screenshots/swagger-ui.png)

OpenAPI 3.1 JSON spec at [localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs).

### Endpoints

#### Single Conversion

```
GET /romannumeral?query={integer}
Header: X-API-Key: <your-key>
```

**Success (200):**
```json
{"input": "1994", "output": "MCMXCIV"}
```

#### Range Conversion (Parallel)

```
GET /romannumeral?min={integer}&max={integer}
Header: X-API-Key: <your-key>
```

**Success (200):**
```json
{
  "conversions": [
    {"input": "1", "output": "I"},
    {"input": "2", "output": "II"},
    {"input": "3", "output": "III"}
  ]
}
```

#### Error Response (400/401/429/500)

```json
{"error": "Bad Request", "message": "Number must be between 1 and 3999, got: 0", "status": 400}
```

#### Actuator (No API Key Required)

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Health check with custom conversion indicator |
| `/actuator/prometheus` | Prometheus-format metrics |
| `/actuator/info` | Application info |

---

## Architecture

> **Detailed sequence diagrams**: [docs/sequence-diagrams.md](docs/sequence-diagrams.md) — single query, range query (parallel), auth failure, rate limiting, startup, observability data flow.

### Clean / Hexagonal Architecture

Dependencies point inward. Domain has zero framework imports.

```
┌─────────────────────────────────────────────────────┐
│                 Infrastructure                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │ Security │ │Observable│ │ Config   │           │
│  │ Filters  │ │ Metrics  │ │ Wiring   │           │
│  └──────────┘ └──────────┘ └──────────┘           │
│                                                      │
│  ┌──────────────────────────────────────────────┐   │
│  │              Web Layer                        │   │
│  │  Controller → DTOs → Error Handler            │   │
│  └──────────────────────┬───────────────────────┘   │
│                         │                            │
│  ┌──────────────────────▼───────────────────────┐   │
│  │           Application Layer                   │   │
│  │  ConvertSingleUseCase  ConvertRangeUseCase    │   │
│  │         ↓ port                ↓ port          │   │
│  └──────────────────────┬───────────────────────┘   │
│                         │                            │
│  ┌──────────────────────▼───────────────────────┐   │
│  │              Domain Layer                     │   │
│  │  RomanNumeralConverter (interface/port)        │   │
│  │  RomanNumeralResult, RomanNumeralRange (VOs)   │   │
│  │  DomainException hierarchy (sealed)            │   │
│  │  *** Zero Spring imports ***                   │   │
│  └──────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

### Dual Converter Strategy

- **Single queries** (`?query=42`): `CachedRomanNumeralConverter` — pre-computed array, O(1) lookup
- **Range queries** (`?min=1&max=3999`): `StandardRomanNumeralConverter` + `ChunkedParallelExecutor` — real parallel computation using virtual threads, 1 chunk per CPU core

> *Range queries use real parallel computation at request time (not cache reads) to directly satisfy the assessment requirement: "use multithreading to compute the values in the range in parallel." See [ADR-0003](docs/adr/0003-dual-converter-strategy.md) for the full trade-off analysis.*

### Algorithm

Descending value-table approach with 13-entry parallel arrays (7 standard symbols + 6 subtractive forms). Greedy subtraction, O(1) bounded (max 15 iterations). Hand-written — no libraries.

The algorithm handles subtractive notation (IV, IX, XL, XC, CD, CM) naturally through the table ordering — no special-case logic needed. For example, 1994: subtract 1000→M, subtract 900→CM, subtract 90→XC, subtract 4→IV = **MCMXCIV**.

---

## Engineering Methodology

### Principles Applied

- **SOLID** — Single Responsibility (separate use cases), Open/Closed (converter interface), Liskov Substitution (cached/standard converters), Interface Segregation (ports), Dependency Inversion (domain defines interfaces)
- **Clean Architecture** — domain layer has zero framework dependencies, testable with `new StandardRomanNumeralConverter()`
- **DRY** — shared `FilterResponseHelper` for filter error responses
- **YAGNI** — no pagination (not required), sequential cache build (4ms not worth parallelizing)

### Design Patterns

| Pattern | Where | Why |
|---------|-------|-----|
| **Strategy** | `RomanNumeralConverter` interface with 2 implementations | Open/Closed — swap converters without touching use cases |
| **Decorator** | `CachedConverter` wraps `StandardConverter` | Adds caching without modifying the algorithm class |
| **Factory Method** | `Response.from()` static methods on DTOs | Mapping logic co-located with DTO |
| **Value Object** | `RomanNumeralResult`, `RomanNumeralRange` (Java records) | Immutable, self-validating, thread-safe |
| **Chain of Responsibility** | 4 servlet filters: Correlation → Logging → Auth → RateLimit | Each filter decides to pass or reject — ordered security pipeline |

### Architecture

| Pattern | Where | Why |
|---------|-------|-----|
| **Clean/Hexagonal (Port/Adapter)** | Domain ports (`RomanNumeralConverter`, `ParallelExecutionPort`) + infrastructure adapters | Domain has zero framework deps. Swap implementations without touching business logic. |
| **Constructor DI** | `@RequiredArgsConstructor` + explicit `ConverterConfig` | All wiring in one file. Testable, immutable fields, no @Primary/@Qualifier. |

---

## Testing Methodology

### Testing Pyramid

```
                ┌──────────┐
                │  Load    │  ← k6 (load, stress, spike, soak)
              ┌─┴──────────┴─┐
              │  Security     │  ← API key, rate limit, headers
            ┌─┴──────────────┴─┐
            │   Integration     │  ← @SpringBootTest, full E2E
          ┌─┴──────────────────┴─┐
          │    API Slice (Web)    │  ← @WebMvcTest, JSON structure
        ┌─┴──────────────────────┴─┐
        │       Unit Tests          │  ← Domain logic, use cases
        └───────────────────────────┘
```

### Test Summary — 192 Tests

| Test Class | Count | What It Tests |
|-----------|-------|---------------|
| `StandardRomanNumeralConverterTest` | 62 | Algorithm: all symbols, subtractive forms, boundaries, exhaustive 1-3999, thread safety |
| `CachedRomanNumeralConverterTest` | 8 | Cache matches standard for all 3999 values, O(1) lookup, thread safety |
| `RomanNumeralRangeTest` | 17 | Self-validating VO: min≥max, out-of-range, record contract |
| `RomanNumeralResultTest` | 7 | Null safety, equality, hashCode |
| `ConvertSingleNumberUseCaseTest` | 2 | Delegation, exception propagation |
| `ConvertRangeUseCaseTest` | 2 | Delegation, converter function passing |
| `ChunkedParallelExecutorTest` | 12 | Ordering, chunking, remainder, error handling, concurrency |
| `RomanNumeralControllerTest` | 43 | Routing, validation, JSON structure, error format, XSS |
| `RomanNumeralIntegrationTest` | 18 | Full E2E: single, range, 1-3999, errors, concurrency |
| `ActuatorIntegrationTest` | 7 | Health, Prometheus metrics, actuator endpoints |
| `ApiSecurityTest` | 11 | API key auth: missing, invalid, valid, actuator bypass, headers |
| `RateLimitTest` | 3 | Token-bucket: single 429, range 429, JSON format |

> *Counts reflect parameterized test expansions (e.g., `StandardRomanNumeralConverterTest` has 10 `@Test` methods that expand to 62 tests via `@ParameterizedTest`).*

### Run Tests

```bash
./mvnw clean test          # run all 192 tests
./mvnw verify              # run tests + JaCoCo coverage check (≥80%)
```

**Coverage: 94.9% line, 95.7% instruction** (JaCoCo threshold: 80%)

---

## Packaging Layout

```
src/main/java/com/adobe/romannumeral/
├── domain/                    ← Core business logic (zero Spring imports)
│   ├── model/                 ← Value Objects (RomanNumeralResult, RomanNumeralRange)
│   ├── service/               ← Ports/interfaces (RomanNumeralConverter, Constants)
│   └── exception/             ← Sealed exception hierarchy
├── application/               ← Use cases + ports
│   ├── usecase/               ← ConvertSingleNumberUseCase, ConvertRangeUseCase
│   └── port/                  ← ParallelExecutionPort interface
├── infrastructure/            ← Framework adapters
│   ├── converter/             ← StandardConverter (algorithm), CachedConverter (O(1))
│   ├── execution/             ← ChunkedParallelExecutor (virtual threads)
│   ├── config/                ← ConverterConfig, SecurityConfig, AppProperties, OpenAPI
│   ├── observability/         ← ConversionMetrics, CorrelationIdFilter, RequestLoggingFilter
│   ├── security/              ← ApiKeyAuthFilter, RateLimitFilter, FilterResponseHelper
│   └── health/                ← ConversionHealthIndicator
└── web/                       ← HTTP layer
    ├── controller/            ← RomanNumeralController
    ├── dto/                   ← SingleConversionResponse, RangeConversionResponse, ErrorResponse
    └── error/                 ← GlobalExceptionHandler
```

**Why this structure:** Dependencies point inward. The domain layer defines interfaces (what the system does), infrastructure provides implementations (how it does it). You can test the converter with `new StandardRomanNumeralConverter()` — no Spring context needed.

---

## Observability

### Unified Grafana (One UI for Everything)

```
Grafana (localhost:3000)
├── App Dashboard       ← conversion rates, latency (p50/p95/p99), errors, JVM
├── Infra Dashboard     ← CPU, memory, network per container
└── Logs Dashboard      ← structured logs searchable by service, level, status
```

#### Application Dashboard
![App Dashboard - Overview](docs/screenshots/app-dashboard-1.png)
![App Dashboard - Latency & JVM](docs/screenshots/app-dashboard-2.png)

#### Infrastructure Dashboard
![Infra Dashboard](docs/screenshots/infra-dashboard.png)

#### Logs Dashboard
![Logs Dashboard - Volume & Errors](docs/screenshots/logs-dashboard-1.png)
![Logs Dashboard - 401 & 429](docs/screenshots/logs-dashboard-2.png)

### Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `roman_conversion_single_total` | Counter | Total single conversions |
| `roman_conversion_single_duration_seconds` | Timer | Single conversion latency |
| `roman_conversion_range_total` | Counter | Total range conversions |
| `roman_conversion_range_duration_seconds` | Timer | Range conversion latency |
| `roman_conversion_range_size` | Distribution | Range sizes requested |
| `roman_conversion_error_total{type=...}` | Counter | Errors by type |

Plus Spring Actuator auto-configured: `http_server_requests_seconds`, JVM metrics, GC metrics.

### Logging

- **Dev**: human-readable console with correlation ID
- **Docker**: structured JSON via logstash-logback-encoder → Promtail → Loki → Grafana
- **Correlation ID**: UUID per request in MDC + `X-Correlation-Id` response header

### Health

`/actuator/health` includes a custom indicator: `convert(1) == "I"`. Proves the business logic works, not just "JVM alive."

![Health Endpoint](docs/screenshots/health-endpoint.png)

---

## Security

### Request Flow

```
Request → CorrelationId → Logging → API Key Auth → Rate Limit → Security Headers → Controller
```

### API Key Authentication

- Header: `X-API-Key: <key>`
- Configured in `application.yml` (production: use secrets manager)
- Actuator + Swagger UI bypass auth
- 401 if missing or invalid

### Rate Limiting (Bucket4j Token-Bucket)

| Endpoint | Limit | Bucket |
|----------|-------|--------|
| Single query (`?query=...`) | 100 req/s | Greedy refill |
| Range query (`?min=...&max=...`) | 10 req/s | Greedy refill |

429 Too Many Requests with `Retry-After: 1` header.

### Security Headers

- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Cache-Control: no-cache, no-store`
- `Strict-Transport-Security` (activates when served over HTTPS)
- CORS: deny all cross-origin by default

> **Production security recommendations** (OAuth2/JWT, mTLS, WAF) are documented in the [Production Roadmap](#production-roadmap).

---

## Load Testing

### k6 Test Suite

| Test | Question | Parameters |
|------|----------|-----------|
| `load-test.js` | Can we handle expected traffic? | 100 VUs, 5 min, p95 < 50ms |
| `stress-test.js` | Where does it break? | Ramp to 500 VUs, 10 min |
| `spike-test.js` | Can we handle sudden bursts? | 10 → 500 → 10 VUs |
| `soak-test.js` | Are there memory leaks? | 50 VUs, 30 min |

### Run

```bash
# Locally (requires k6 installed)
k6 run k6/load-test.js

# Via Docker (app must be running via docker compose up)
docker compose -f k6/docker-compose.k6.yml run k6-load
docker compose -f k6/docker-compose.k6.yml run k6-stress
docker compose -f k6/docker-compose.k6.yml run k6-spike
docker compose -f k6/docker-compose.k6.yml run k6-soak
```

> **Tip:** While k6 runs, open the Grafana App Dashboard at [localhost:3000](http://localhost:3000) — request rates, latency percentiles, error rates, and rate limit rejections all update in real-time.

---

## Docker

### Services

| Service | Port | Purpose |
|---------|------|---------|
| roman-numeral-service | 8080 | Application |
| Prometheus | 9090 | Metrics scraping (15s interval) |
| Grafana | 3000 | Dashboards + log exploration (admin/admin) |
| Loki | 3100 | Log aggregation |
| Promtail | — | Log collection from containers |
| cAdvisor | 8081 | Container metrics (CPU, memory, network) |
| Node Exporter | 9100 | Host metrics |

### Commands

```bash
docker compose up -d           # start everything
docker compose logs -f roman-numeral-service  # follow app logs
docker compose down            # graceful shutdown
```

---

## CI/CD

### GitHub Actions (Primary)

Pipeline: **Checkout → Build → Test → Coverage Check → Docker Build → Docker Push**

- Triggers on push to `main` and pull requests
- Quality gate: JaCoCo ≥ 80% line coverage
- Test + coverage reports archived as artifacts
- Docker push optional (requires `DOCKER_USERNAME`/`DOCKER_PASSWORD` secrets)
- `Jenkinsfile` also included for enterprise CI/CD environments (same stages — see [ADR-0008](docs/adr/0008-github-actions-over-jenkins.md) for decision rationale)

---

## AI Integration

### AI Development Guide (Multi-Tool)

Single source of truth for AI-assisted development: [`docs/ai-development-guide.md`](docs/ai-development-guide.md).

Generate conventions for your AI tool of choice:
```bash
./scripts/sync-ai-conventions.sh claude    # if you use Claude Code
./scripts/sync-ai-conventions.sh cursor    # if you use Cursor
./scripts/sync-ai-conventions.sh copilot   # if you use GitHub Copilot
./scripts/sync-ai-conventions.sh gemini    # if you use Gemini
./scripts/sync-ai-conventions.sh windsurf  # if you use Windsurf
./scripts/sync-ai-conventions.sh all       # generate all at once
```

| Tool | Generated File |
|------|---------------|
| Claude Code | `CLAUDE.md` |
| Cursor | `.cursorrules` |
| GitHub Copilot | `.github/copilot-instructions.md` |
| Gemini | `GEMINI.md` |
| Windsurf | `.windsurfrules` |

Open the project in your AI IDE — it picks up the conventions automatically.

### MCP Server (AI-Callable Tools)

Any MCP-compatible AI agent can use the conversion service as a tool.

**Tools:** `convert_number`, `convert_range` — ask your AI *"Convert 1994 to a Roman numeral"* and it calls your local service.

```bash
# 1. Install (one-time)
cd mcp-server && python3 -m venv venv && source venv/bin/activate && pip install -r requirements.txt && cd ..

# 2. Make sure the service is running (via mvnw or docker compose)

# 3. Connect to your AI tool (pick one):
```

| AI Tool | Setup |
|---------|-------|
| **Claude Code** (tested) | `claude mcp add` from project root — [full instructions](mcp-server/README.md#claude-code-cli) |
| **Claude Desktop** | JSON config — [instructions](mcp-server/README.md#claude-desktop) |
| **Other tools** (Cursor, VS Code, Windsurf) | Standard stdio MCP — [generic setup](mcp-server/README.md#other-mcp-compatible-tools-cursor-vs-code-windsurf-etc) |

> **Important:** Use venv's Python (`mcp-server/venv/bin/python3`) and full absolute paths. Run `claude mcp add` from the project root, not `mcp-server/`. Details: [`mcp-server/README.md`](mcp-server/README.md)

### AI Code Review Agent

Reviews code against `ai-development-guide.md` conventions using any LLM provider.

```bash
# Setup (one-time)
cd scripts && python3 -m venv venv && source venv/bin/activate && pip install -r requirements.txt && cd ..

# Set your API key
export ANTHROPIC_API_KEY=sk-ant-your-key

# Dry run (see what would be sent, no API call)
python3 scripts/ai-review.py --all --dry-run

# Review entire project (~$0.19 with Sonnet)
python3 scripts/ai-review.py --all --model claude-sonnet-4-20250514

# Review a single file (~$0.03)
python3 scripts/ai-review.py src/main/java/com/adobe/romannumeral/web/controller/RomanNumeralController.java --model claude-sonnet-4-20250514
```

**Tested with:** Anthropic Claude Sonnet 4. The script also supports OpenAI (`--model gpt-4o`) and Google Gemini (`--model gemini-1.5-pro`) — provider is auto-detected from which API key is set. OpenAI and Gemini are implemented but not tested.

### Data Privacy Notice

- **Code Review Agent**: Sends source code to your configured LLM provider when you explicitly run the script. Warns before sending.
- **MCP Server**: Calls the local Roman numeral service API only. Does NOT send data to external services.
- No telemetry. No background calls. No data sent without your explicit action.

---

## Architecture Decision Records

All significant technical decisions are documented in [`docs/adr/`](docs/adr/):

| ADR | Decision |
|-----|----------|
| [0001](docs/adr/0001-clean-hexagonal-architecture.md) | Clean/Hexagonal Architecture over Layered |
| [0002](docs/adr/0002-java21-spring-boot-3.4.md) | Java 21 + Spring Boot 3.4.1 |
| [0003](docs/adr/0003-dual-converter-strategy.md) | Dual Converter Strategy (Cached + Standard) |
| [0004](docs/adr/0004-chunked-parallelism.md) | Chunked Parallelism (CPU-core-based) |
| [0005](docs/adr/0005-loki-grafana-over-elk-splunk.md) | Loki + Grafana over ELK/Splunk |
| [0006](docs/adr/0006-unified-grafana-observability.md) | Unified Grafana for metrics, logs, infra |
| [0007](docs/adr/0007-api-key-auth-bucket4j-rate-limiting.md) | API Key + Bucket4j Rate Limiting |
| [0008](docs/adr/0008-github-actions-over-jenkins.md) | GitHub Actions over Jenkins |
| [0009](docs/adr/0009-k6-over-gatling.md) | k6 over Gatling for Load Testing |
| [0010](docs/adr/0010-multi-tool-ai-development-guide.md) | Multi-Tool AI Development Guide |
| [0011](docs/adr/0011-mcp-server-for-ai-composability.md) | MCP Server for AI Composability |
| [0012](docs/adr/0012-local-code-review-agent.md) | Local Code Review Agent |

---

## Dependency Attribution

| Dependency | Purpose | License |
|-----------|---------|---------|
| `spring-boot-starter-web` | Embedded Tomcat, Spring MVC, JSON serialization | Apache 2.0 |
| `spring-boot-starter-actuator` | Health, info, metrics, prometheus endpoints | Apache 2.0 |
| `spring-boot-starter-security` | Security headers, CORS, filter chain | Apache 2.0 |
| `spring-boot-starter-validation` | Jakarta Bean Validation (Hibernate Validator) | Apache 2.0 |
| `micrometer-registry-prometheus` | Prometheus metrics format (Grafana) | Apache 2.0 |
| `logstash-logback-encoder` | JSON-structured logging (Loki) | Apache 2.0 |
| `springdoc-openapi-starter-webmvc-ui` | OpenAPI 3.1 spec + Swagger UI | Apache 2.0 |
| `bucket4j-core` | Token-bucket rate limiting | Apache 2.0 |
| `lombok` | `@Slf4j`, `@RequiredArgsConstructor` | MIT |
| `spring-boot-starter-test` | JUnit 5, Mockito, MockMvc, AssertJ | Apache 2.0 |
| `spring-security-test` | Security test utilities | Apache 2.0 |
| `jacoco-maven-plugin` | Code coverage reports, 80% threshold | EPL 2.0 |

---

## Production Roadmap

Prioritized list of what a principal engineer would add before and after going to production.

### P0 — Before Production (Must Have)

| Item | Details |
|------|---------|
| **Alerting** | Grafana alerting rules routed to PagerDuty/Slack. Key thresholds: error rate > 1% (5 min), single p95 > 50ms (5 min), range p95 > 200ms (5 min), JVM heap > 80% warning / > 90% critical, container restarts > 0, 429 rejection rate > 30% (10 min), host CPU > 85%, host memory > 90%, host disk > 85%. |
| **Secrets Management** | Move API keys from `application.yml` to HashiCorp Vault or AWS Secrets Manager. No secrets in config files or environment variables in production. |
| **HTTPS / TLS Termination** | TLS at load balancer or API Gateway. HSTS header is already configured — needs actual HTTPS to activate. |

### P1 — First Month in Production

| Item | Details |
|------|---------|
| **Kubernetes + Helm** | Deployment manifests, HPA auto-scaling, liveness/readiness probes. Docker image is already K8s-ready (HEALTHCHECK, graceful shutdown, non-root user, externalized config). |
| **OAuth2 / JWT** | At API Gateway layer (Kong, AWS API Gateway) for multi-tenant authentication. API key auth is sufficient for single-tenant; OAuth2 needed for multiple consumers with different scopes. |
| **Distributed Tracing** | OpenTelemetry integration for cross-service tracing. Correlation ID provides basic single-service tracing — OpenTelemetry extends it across service boundaries. |

### P2 — As Needed

| Item | Details |
|------|---------|
| **WAF** | Cloud provider WAF (AWS WAF, Cloudflare) for DDoS and bot protection. Only needed if internet-facing. |
| **mTLS** | Service mesh (Istio, Linkerd) for network-level encryption between services. Only needed in multi-service architectures. |
| **Streaming Response** | `StreamingResponseBody` for memory-efficient large range responses. Not needed at current max (3999 values = ~20KB compressed). |
| **ETag / Conditional Requests** | Roman numerals are immutable — `ETag` would enable client caching with `304 Not Modified`. Reduces bandwidth on repeated requests. |

### Pagination — Why Not Implemented

The client already controls range size via `min`/`max` parameters — effectively client-driven pagination. Max range (3999 values) compresses to ~20KB with gzip. Adding server-side pagination would change the API contract from what the assessment specifies.

---

## GitHub Repository

[https://github.com/dhinupriya/roman-numeral-service](https://github.com/dhinupriya/roman-numeral-service)

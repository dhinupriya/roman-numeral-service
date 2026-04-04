# Sequence Diagrams

Detailed request flows for the Roman Numeral Conversion Service.

---

## Single Conversion Flow

```mermaid
sequenceDiagram
    participant Client
    participant CorrelationFilter
    participant LoggingFilter
    participant AuthFilter
    participant RateLimitFilter
    participant Controller
    participant UseCase
    participant CachedConverter
    participant Metrics

    Client->>CorrelationFilter: GET /romannumeral?query=1994<br/>X-API-Key: test-api-key-1
    CorrelationFilter->>CorrelationFilter: Generate UUID → MDC
    CorrelationFilter->>LoggingFilter: Forward
    LoggingFilter->>LoggingFilter: Record start time
    LoggingFilter->>AuthFilter: Forward
    AuthFilter->>AuthFilter: Validate X-API-Key ✅
    AuthFilter->>RateLimitFilter: Forward
    RateLimitFilter->>RateLimitFilter: tryConsume(1) from singleBucket ✅
    RateLimitFilter->>Controller: Forward
    Controller->>Controller: parseInteger("1994") → 1994
    Controller->>UseCase: execute(1994)
    UseCase->>Metrics: recordSingle(...)
    Metrics->>CachedConverter: convert(1994)
    CachedConverter->>CachedConverter: cache[1994] → "MCMXCIV" (O(1))
    CachedConverter-->>Metrics: RomanNumeralResult("1994", "MCMXCIV")
    Metrics-->>UseCase: Result + timer recorded
    UseCase-->>Controller: RomanNumeralResult
    Controller->>Controller: SingleConversionResponse.from(result)
    Controller-->>LoggingFilter: 200 {"input":"1994","output":"MCMXCIV"}
    LoggingFilter->>LoggingFilter: Log: GET /romannumeral?query=1994 → 200 (2ms, 38B)
    LoggingFilter-->>CorrelationFilter: Response
    CorrelationFilter->>CorrelationFilter: Add X-Correlation-Id header<br/>Clean MDC
    CorrelationFilter-->>Client: 200 OK + JSON + X-Correlation-Id
```

---

## Range Conversion Flow (Parallel)

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant RangeUseCase
    participant Metrics
    participant Executor
    participant VT1 as Virtual Thread 1
    participant VT2 as Virtual Thread 2
    participant VTn as Virtual Thread N
    participant StandardConverter

    Client->>Controller: GET /romannumeral?min=1&max=3999<br/>X-API-Key: test-api-key-1
    Note over Controller: Filters run first (same as single)
    Controller->>Controller: parseInteger("1") → 1<br/>parseInteger("3999") → 3999<br/>Validate range size ≤ 3999
    Controller->>Controller: new RomanNumeralRange(1, 3999)<br/>Self-validates: 1 < 3999 ✅
    Controller->>RangeUseCase: execute(range)
    RangeUseCase->>Metrics: recordRange(3999, ...)
    Metrics->>Executor: executeRange(1, 3999, converter::convert)
    
    Note over Executor: Split into chunks (8 CPU cores)
    Executor->>Executor: Chunk 1: [1-500]<br/>Chunk 2: [501-1000]<br/>...<br/>Chunk 8: [3501-3999]
    
    par Parallel Execution
        Executor->>VT1: supplyAsync(chunk 1)
        VT1->>StandardConverter: convert(1), convert(2), ... convert(500)
        StandardConverter-->>VT1: List[500 results]
    and
        Executor->>VT2: supplyAsync(chunk 2)
        VT2->>StandardConverter: convert(501), ... convert(1000)
        StandardConverter-->>VT2: List[500 results]
    and
        Executor->>VTn: supplyAsync(chunk 8)
        VTn->>StandardConverter: convert(3501), ... convert(3999)
        StandardConverter-->>VTn: List[499 results]
    end

    Note over Executor: Join futures in creation order<br/>→ ascending order preserved<br/>→ no sorting needed
    Executor-->>Metrics: List[3999 results] ordered
    Metrics-->>RangeUseCase: Results + timer + range size recorded
    RangeUseCase-->>Controller: List<RomanNumeralResult>
    Controller->>Controller: RangeConversionResponse.from(results)
    Controller-->>Client: 200 {"conversions":[...3999 items...]}
```

---

## Authentication Failure Flow

```mermaid
sequenceDiagram
    participant Client
    participant CorrelationFilter
    participant LoggingFilter
    participant AuthFilter
    participant Controller

    Client->>CorrelationFilter: GET /romannumeral?query=42<br/>(no X-API-Key)
    CorrelationFilter->>CorrelationFilter: Generate UUID → MDC
    CorrelationFilter->>LoggingFilter: Forward
    LoggingFilter->>LoggingFilter: Record start time
    LoggingFilter->>AuthFilter: Forward
    AuthFilter->>AuthFilter: Check X-API-Key header → MISSING
    AuthFilter->>AuthFilter: FilterResponseHelper.writeErrorResponse()
    
    Note over AuthFilter: Request STOPS here<br/>Controller never reached<br/>Rate limit tokens not consumed
    
    AuthFilter-->>LoggingFilter: 401 {"error":"Unauthorized","message":"Missing API key..."}
    LoggingFilter->>LoggingFilter: Log: GET /romannumeral?query=42 → 401 (1ms, 112B)
    LoggingFilter-->>CorrelationFilter: Response
    CorrelationFilter->>CorrelationFilter: Add X-Correlation-Id header<br/>Clean MDC
    CorrelationFilter-->>Client: 401 Unauthorized + JSON + X-Correlation-Id
```

---

## Rate Limit Exceeded Flow

```mermaid
sequenceDiagram
    participant Client
    participant CorrelationFilter
    participant LoggingFilter
    participant AuthFilter
    participant RateLimitFilter
    participant Controller

    Client->>CorrelationFilter: GET /romannumeral?query=42<br/>X-API-Key: test-api-key-1
    CorrelationFilter->>LoggingFilter: Forward (UUID in MDC)
    LoggingFilter->>AuthFilter: Forward
    AuthFilter->>AuthFilter: Validate API key ✅
    AuthFilter->>RateLimitFilter: Forward
    RateLimitFilter->>RateLimitFilter: tryConsume(1) from singleBucket → FALSE<br/>(bucket exhausted)
    RateLimitFilter->>RateLimitFilter: rateLimitCounter.increment()
    RateLimitFilter->>RateLimitFilter: FilterResponseHelper.writeErrorResponse()
    
    Note over RateLimitFilter: Request STOPS here<br/>Controller never reached
    
    RateLimitFilter-->>LoggingFilter: 429 + Retry-After: 1
    LoggingFilter->>LoggingFilter: Log: GET → 429 (0ms)
    LoggingFilter-->>CorrelationFilter: Response
    CorrelationFilter-->>Client: 429 Too Many Requests + Retry-After: 1
```

---

## Domain Validation Error Flow

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant UseCase
    participant CachedConverter
    participant ExceptionHandler
    participant Metrics

    Client->>Controller: GET /romannumeral?query=0<br/>X-API-Key: test-api-key-1
    Note over Controller: Filters pass (auth ✅, rate limit ✅)
    Controller->>Controller: parseInteger("0") → 0
    Controller->>UseCase: execute(0)
    UseCase->>CachedConverter: convert(0)
    CachedConverter->>CachedConverter: 0 < MIN_VALUE (1)
    CachedConverter-->>UseCase: throws InvalidInputException
    UseCase-->>Controller: throws InvalidInputException
    Controller-->>ExceptionHandler: Exception propagates
    ExceptionHandler->>Metrics: recordError("InvalidInput")
    ExceptionHandler->>ExceptionHandler: buildResponse(400, message)
    ExceptionHandler-->>Client: 400 {"error":"Bad Request","message":"Number must be between 1 and 3999, got: 0"}
```

---

## Startup Flow (Cache Pre-computation)

```mermaid
sequenceDiagram
    participant SpringBoot
    participant ConverterConfig
    participant StandardConverter
    participant CachedConverter
    participant HealthIndicator

    SpringBoot->>ConverterConfig: Initialize beans
    ConverterConfig->>StandardConverter: new StandardRomanNumeralConverter()
    ConverterConfig->>CachedConverter: new CachedRomanNumeralConverter(standard)
    CachedConverter->>CachedConverter: Create String[4000] array
    
    Note over CachedConverter: @PostConstruct
    CachedConverter->>CachedConverter: initializeCache()
    
    loop i = 1 to 3999 (sequential, ~4ms)
        CachedConverter->>StandardConverter: convert(i)
        StandardConverter-->>CachedConverter: RomanNumeralResult
        CachedConverter->>CachedConverter: cache[i] = result.output()
    end
    
    CachedConverter->>CachedConverter: Log: "Pre-computed 3999 values in 1ms"
    
    Note over CachedConverter: Cache is now immutable<br/>Read-only during request handling<br/>Thread-safe via happens-before
    
    ConverterConfig->>ConverterConfig: Wire use cases:<br/>Single → CachedConverter<br/>Range → StandardConverter + Executor
    
    SpringBoot->>HealthIndicator: Initialize
    HealthIndicator->>CachedConverter: convert(1)
    CachedConverter-->>HealthIndicator: "I" ✅
    
    SpringBoot->>SpringBoot: Application ready — accepting requests
```

---

## Observability Data Flow

```mermaid
sequenceDiagram
    participant App as Roman Numeral Service
    participant Prometheus
    participant cAdvisor
    participant NodeExporter
    participant Promtail
    participant Loki
    participant Grafana

    Note over App: Application running

    Note over App, Grafana: 1. Application Metrics Flow
    App->>App: Micrometer records business metrics
    Prometheus->>App: Scrape /actuator/prometheus (every 15s)
    App-->>Prometheus: roman_conversion_*, jvm_*, http_*
    Grafana->>Prometheus: Query metrics
    Prometheus-->>Grafana: App Dashboard (15 panels)

    Note over App, Grafana: 2. Infrastructure Metrics Flow
    cAdvisor->>cAdvisor: Collect container CPU, memory, network
    NodeExporter->>NodeExporter: Collect host CPU, memory, disk
    Prometheus->>cAdvisor: Scrape (every 15s)
    Prometheus->>NodeExporter: Scrape (every 15s)
    Grafana->>Prometheus: Query infra metrics
    Prometheus-->>Grafana: Infra Dashboard (7 panels)

    Note over App, Grafana: 3. Logs Flow
    App->>App: Logback JSON → stdout
    Promtail->>App: Tail container logs (Docker SD)
    Promtail->>Loki: Push structured logs
    Grafana->>Loki: Query logs (LogQL)
    Loki-->>Grafana: Logs Dashboard (6 panels)

    Note over Grafana: One UI for everything:<br/>App metrics + Infra metrics + Logs
```

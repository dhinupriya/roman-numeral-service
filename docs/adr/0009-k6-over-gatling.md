# ADR-0009: k6 over Gatling for Load Testing

## Status
Accepted

## Context
Need load/performance testing to verify the service handles expected traffic, stress, spikes, and sustained load. Must integrate with our existing Grafana-based observability stack.

## Options Considered
1. **Gatling** — JVM-based (Scala DSL), generates HTML reports, integrates with Maven. Industry standard but adds Scala ecosystem to a Java project.
2. **k6** — by Grafana Labs, JavaScript-based, lightweight Go binary, native Prometheus integration. Results visible in same Grafana we already use.
3. **JMeter** — XML-based, GUI for building tests, CLI for running. Widely known but heavy and verbose.

## Decision
k6 — by Grafana Labs, JavaScript test scripts, Docker-based execution.

## Consequences
- **Gain:** Stack coherence — same vendor ecosystem (Grafana Labs: Grafana + Loki + Promtail + k6). Load test results visible in Grafana alongside app metrics. JavaScript scripts are readable by any developer. Runs as Docker container — no local installation needed.
- **Trade-off:** Not JVM-based — can't share code with the Java project. Acceptable since load tests are infrastructure scripts, not application code.
- **Enables:** Four test types (load, stress, spike, soak) each answering a different operational question. Docker Compose for easy execution.

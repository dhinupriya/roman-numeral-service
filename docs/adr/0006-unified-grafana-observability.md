# ADR-0006: Unified Grafana for Metrics, Logs, and Infrastructure

## Status
Accepted

## Context
We have multiple observability signals: application metrics, infrastructure metrics (CPU, memory), structured logs, and load test results. Need to decide whether to use separate tools or unify under one platform.

## Options Considered
1. **Separate tools** — Grafana for metrics, Kibana for logs, custom for infra. Each specialized but fragmented — multiple UIs, multiple learning curves, context switching during debugging.
2. **Unified Grafana** — one UI for everything. Prometheus for app + infra metrics, Loki for logs, k6 results to Prometheus. All datasources auto-provisioned.

## Decision
Unified Grafana with four pre-built views:
- **App Dashboard** — conversion rates, latencies (p50/p95/p99), errors, JVM memory (Micrometer → Prometheus)
- **Infra Dashboard** — CPU, memory, network per container, host stats (cAdvisor + Node Exporter → Prometheus)
- **Load Test Dashboard** — k6 results under load (k6 → Prometheus)
- **Logs (Explore)** — structured JSON logs searchable by correlation ID (Promtail → Loki)

## Consequences
- **Gain:** One tool to learn, one URL to bookmark, one place to investigate incidents. See latency spike → switch to logs → filter by time window and correlation ID. Coherent stack from the same vendor (Grafana Labs: Grafana + Loki + Promtail + k6).
- **Trade-off:** Grafana's log exploration is less feature-rich than dedicated tools like Kibana. Acceptable for this service's scale.
- **Enables:** Pre-provisioned dashboards that work out of the box with `docker compose up`. Reviewer sees live metrics immediately without manual configuration.

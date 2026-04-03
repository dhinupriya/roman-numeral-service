# ADR-0005: Loki + Grafana over ELK/Splunk for Log Aggregation

## Status
Accepted

## Context
Extension 3 requires logging as part of production readiness. Need a log aggregation solution that runs in Docker Compose, has a UI for searching logs, and integrates with our existing monitoring stack.

## Options Considered
1. **Splunk** — industry standard, powerful search, great UI. But proprietary, heavy, complex Docker setup, separate UI from Grafana.
2. **ELK (Elasticsearch + Logstash + Kibana)** — open-source, powerful search, Kibana is excellent. But Elasticsearch needs 2GB+ RAM, heavy for a home assessment, introduces a second UI (Kibana) alongside Grafana.
3. **Loki + Grafana (PLG stack)** — built by Grafana Labs. Lightweight, same Grafana UI for both metrics AND logs. Promtail collects container logs automatically. Minimal config.

## Decision
Loki + Grafana with Promtail for log collection. Structured JSON logs via logstash-logback-encoder → stdout → Promtail → Loki → Grafana Explore.

## Consequences
- **Gain:** One UI (Grafana) for metrics, logs, and infra monitoring. Lightweight — runs in Docker Compose without 2GB+ RAM. Modern observability stack. Reviewer sees a unified story, not a patchwork.
- **Trade-off:** Loki's query language (LogQL) is less powerful than Elasticsearch's full-text search. Sufficient for our use case.
- **Enables:** Unified Grafana experience — see a latency spike in the App Dashboard, then switch to Explore to search logs for that time window by correlation ID.

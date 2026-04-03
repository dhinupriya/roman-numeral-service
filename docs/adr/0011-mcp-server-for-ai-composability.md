# ADR-0011: MCP Server for AI Composability

## Status
Accepted

## Context
AI agents are becoming first-class API consumers. The service should be callable by both humans (REST) and AI agents (MCP). Need to decide: embed MCP in the Java service or build a separate MCP server.

## Options Considered
1. **Embed MCP in Spring Boot** — add MCP protocol handling to the Java service. Mixes HTTP API concerns with AI protocol concerns. Java MCP libraries are immature.
2. **Standalone Python MCP server** — thin HTTP client calling the REST API. Official MCP Python SDK is mature. Separate deployment, separate concerns.
3. **Standalone TypeScript MCP server** — official SDK available. Requires Node.js + TypeScript compilation.

## Decision
Standalone Python MCP server (`mcp-server/`). Thin HTTP client — calls REST API, returns results. No shared code with Java service.

## Consequences
- **Gain:** Clean separation. Python MCP SDK is mature. Any MCP-compatible AI agent can use the service. Reviewer can test by connecting Claude Desktop.
- **Trade-off:** Separate process to run. Acceptable — it's a development/integration tool, not a production service.
- **Security:** API key from .env (gitignored), input validation before HTTP call, 5s timeout, no eval/exec.

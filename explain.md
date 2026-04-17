# Use of AI in This Project

## AI Tool

**Claude Code** (Anthropic) — Claude Opus model, used via the CLI terminal throughout the project.

**Why Claude Code:** Terminal-native tool that operates inside the project directory with full file system access. It works as a pair-programming partner — I control when and what it generates, rather than reacting to autocomplete suggestions. It also supports the MCP protocol natively, which directly enabled the MCP server built into this project.

**Why Opus:** The strongest reasoning model available, suited for architecture decisions and complex multi-file changes. For an assessment where design quality matters more than generation speed, Opus over Sonnet was the right trade-off.

## Development Approach

AI was used as a **pair-programming partner**, not as a code generator. The workflow followed a deliberate pattern:

1. **Discuss and decide** — I described what I wanted to achieve, explored trade-offs conversationally, and made the final call on every architectural decision.
2. **Plan before code** — The full architecture (clean/hexagonal, 6-phase implementation plan, domain model, interfaces) was designed and agreed upon before any code was written. This ensured AI-generated code followed the intended architecture from the start — not the other way around.
3. **Implement incrementally** — Each phase was pair-programmed: I specified the intent, AI produced code, I reviewed every file, understood every line, and iterated until satisfied. The 48 commits in the history reflect this incremental progression — not a monolithic AI dump.
4. **Test alongside** — 192 tests were written alongside implementation, not bolted on after. AI helped enumerate edge cases and parameterized inputs; I verified correctness and coverage (94.9% line, 95.7% instruction).
5. **Refactor for clarity** — Multiple passes after functionality was correct: improving naming, removing unnecessary complexity, tightening error handling.

## What I Own vs What AI Accelerated

**I own every decision.** Specifically:

- Architecture: Clean/Hexagonal with strict layer boundaries ([ADR-0001](docs/adr/0001-clean-hexagonal-architecture.md))
- Dual converter strategy: cached O(1) for single queries, real parallel computation for ranges ([ADR-0003](docs/adr/0003-dual-converter-strategy.md))
- Chunked parallelism: 1 virtual thread per CPU core, not per number ([ADR-0004](docs/adr/0004-chunked-parallelism.md))
- Observability stack: Loki + Grafana over ELK/Splunk ([ADR-0005](docs/adr/0005-loki-grafana-over-elk-splunk.md))
- Security model: filter ordering, rate limiting before auth waste prevention ([ADR-0007](docs/adr/0007-api-key-auth-bucket4j-rate-limiting.md))
- CI/CD: GitHub Actions over Jenkins ([ADR-0008](docs/adr/0008-github-actions-over-jenkins.md))
- Load testing: k6 over Gatling ([ADR-0009](docs/adr/0009-k6-over-gatling.md))

Each decision is documented with context, options considered, and rationale in the [Architecture Decision Records](docs/adr/).

**AI accelerated the mechanical work:** boilerplate code generation from clear specifications, test case enumeration, configuration files (Docker Compose, Prometheus, Grafana provisioning), documentation formatting, and Mermaid diagram syntax.

## Why This Approach

I treat AI the same way I treat any powerful tool — with intentionality:

- **Planning-first prevents architectural drift.** When AI generates code against a well-defined architecture and explicit conventions, the output fits. Without upfront design, AI produces generic Spring Boot patterns that don't respect domain boundaries.
- **Understanding is non-negotiable.** Every pattern in this codebase (sealed exception hierarchies, explicit bean wiring via `ConverterConfig`, `ChunkedParallelExecutor` design) exists because I understand why it's the right choice — not because AI suggested it and I accepted it.
- **Speed without shortcuts.** AI eliminated hours of typing but zero minutes of thinking. The result is a codebase I can defend line-by-line in a design review.

## AI-Powered Features Built Into the Project

Beyond using AI for development, I built three features that demonstrate how AI integrates with the service itself:

### 1. MCP Server ([ADR-0011](docs/adr/0011-mcp-server-for-ai-composability.md))

A standalone Python MCP server (`mcp-server/`) that exposes the Roman Numeral API as AI-callable tools. Any MCP-compatible agent (Claude Code, Claude Desktop, Cursor, etc.) can invoke `convert_number` and `convert_range` as native tools.

**Why:** AI agents are becoming first-class API consumers. Wrapping a REST API with MCP makes the service composable in AI workflows — an agent can reason about conversions rather than constructing HTTP requests.

### 2. AI Development Guide + Sync Script ([ADR-0010](docs/adr/0010-multi-tool-ai-development-guide.md))

A single source of truth (`docs/ai-development-guide.md`) containing all project conventions — architecture rules, code patterns, security requirements, testing standards. A shell script (`scripts/sync-ai-conventions.sh`) distributes it to five AI tool formats: `CLAUDE.md`, `.cursorrules`, `copilot-instructions.md`, `GEMINI.md`, `.windsurfrules`. Supports syncing to all tools at once or a single tool (e.g., `./scripts/sync-ai-conventions.sh claude`), so developers only generate what they need.

**Why:** Without project-specific conventions, every AI tool generates generic code that violates the architecture. One file, synced everywhere, ensures any team member using any AI coding tool gets architecture-aware suggestions immediately.

### 3. Code Review Agent ([ADR-0012](docs/adr/0012-local-code-review-agent.md))

A provider-agnostic CLI (`scripts/ai-review.py`) that reviews code against the project's own conventions. Supports Anthropic, OpenAI, and Google — auto-detects whichever API key is set. Flexible scope: reviews staged changes by default, specific files by name, or the entire project with `--all`. Includes dry-run mode, file size limits, and a security warning before sending code externally.

**Why:** A local CLI that any reviewer can test in 30 seconds (set one env var, run one command) is more valuable than a CI-integrated review that requires access to repo secrets.

## Future AI Integration

The current AI features are a foundation. Natural next steps:

- **CI-integrated code review** — Promote `ai-review.py` into GitHub Actions, triggered on pull requests. The local-first design ([ADR-0012](docs/adr/0012-local-code-review-agent.md)) was intentionally built to support this migration.
- **MCP tool expansion** — Expose health checks, Prometheus metrics, and Grafana queries as MCP tools, enabling an AI agent to not only use the service but also monitor and diagnose it.
- **Pre-commit hooks** — Wire the AI review agent as a git hook so every commit is automatically validated against conventions before it's pushed.

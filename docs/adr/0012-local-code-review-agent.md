# ADR-0012: Local Code Review Agent over GitHub Actions CI

## Status
Accepted

## Context
Want AI-powered code review. Two approaches: in GitHub Actions (automated on PRs) or as a local CLI tool (developer runs manually). The reviewer needs to be able to test it.

## Options Considered
1. **GitHub Actions AI review** — automated on PRs. Reviewer can't test it (needs API key in repo secrets they don't control).
2. **Local CLI script** — reviewer sets one env var, runs one command, sees the review instantly. Can also be wired into CI later.
3. **Pre-commit hook** — runs on every commit. Too aggressive — developers may not want AI review on every commit.

## Decision
Local Python CLI (`scripts/ai-review.py`). Provider-agnostic — detects ANTHROPIC_API_KEY, OPENAI_API_KEY, or GOOGLE_API_KEY. Reviews code against `ai-development-guide.md`.

## Consequences
- **Gain:** Reviewer tests in 30 seconds: set API key → run script → see review. No access to repo secrets needed. Works with any LLM provider.
- **Trade-off:** Not automated in CI (manual trigger). Document that teams can wire it into CI.
- **Security:** API key from env only, warns before sending code, dry-run mode, file size limits, no eval of LLM output.

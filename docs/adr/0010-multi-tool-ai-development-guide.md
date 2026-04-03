# ADR-0010: Multi-Tool AI Development Guide

## Status
Accepted

## Context
Teams use different AI tools (Claude Code, Cursor, GitHub Copilot, Gemini, Windsurf). Each reads a different conventions file. Without project-specific conventions, AI tools generate code that violates the architecture.

## Options Considered
1. **Separate files per tool** — manually maintain 5 files. Content drifts apart over time.
2. **Single source of truth + sync script** — one `ai-development-guide.md`, a shell script distributes to all tool-specific files. Auto-generated header prevents direct edits.
3. **Symlinks** — GitHub doesn't render symlinked files, AI tools may not follow them.

## Decision
Single source (`docs/ai-development-guide.md`) + `scripts/sync-ai-conventions.sh` distributes to CLAUDE.md, .cursorrules, copilot-instructions.md, GEMINI.md, .windsurfrules. Support individual or all-at-once sync.

## Consequences
- **Gain:** One file to maintain, all tools stay in sync. New developer opens project in any AI IDE and gets architecture-aware suggestions immediately.
- **Trade-off:** Need to run sync script after editing the guide. Auto-generated header reminds developers not to edit tool files directly.

#!/usr/bin/env bash
# ============================================================================
# Sync AI Development Guide to all tool-specific convention files
#
# Single source of truth: docs/ai-development-guide.md
# Distributes to: CLAUDE.md, .cursorrules, copilot-instructions, GEMINI.md, .windsurfrules
#
# Usage:
#   ./scripts/sync-ai-conventions.sh all       # sync to all tools
#   ./scripts/sync-ai-conventions.sh claude     # sync to CLAUDE.md only
#   ./scripts/sync-ai-conventions.sh cursor     # sync to .cursorrules only
#   ./scripts/sync-ai-conventions.sh copilot    # sync to copilot-instructions.md
#   ./scripts/sync-ai-conventions.sh gemini     # sync to GEMINI.md only
#   ./scripts/sync-ai-conventions.sh windsurf   # sync to .windsurfrules only
# ============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SOURCE="$PROJECT_ROOT/docs/ai-development-guide.md"

if [ ! -f "$SOURCE" ]; then
    echo "ERROR: Source file not found: $SOURCE"
    exit 1
fi

HEADER="# Auto-generated from docs/ai-development-guide.md — do not edit directly.
# Edit docs/ai-development-guide.md and run: ./scripts/sync-ai-conventions.sh all
"

sync_tool() {
    local tool="$1"
    local target=""

    case "$tool" in
        claude)   target="$PROJECT_ROOT/CLAUDE.md" ;;
        cursor)   target="$PROJECT_ROOT/.cursorrules" ;;
        copilot)  target="$PROJECT_ROOT/.github/copilot-instructions.md"
                  mkdir -p "$PROJECT_ROOT/.github" ;;
        gemini)   target="$PROJECT_ROOT/GEMINI.md" ;;
        windsurf) target="$PROJECT_ROOT/.windsurfrules" ;;
        *)
            echo "Unknown tool: $tool"
            echo "Valid tools: claude, cursor, copilot, gemini, windsurf, all"
            return 1 ;;
    esac

    echo "$HEADER" > "$target"
    cat "$SOURCE" >> "$target"
    echo "  ✅ $tool → $target"
}

if [ $# -eq 0 ]; then
    echo "Usage: $0 <tool|all>"
    echo ""
    echo "Tools: claude, cursor, copilot, gemini, windsurf, all"
    echo ""
    echo "Source: docs/ai-development-guide.md"
    echo "Syncs project conventions to tool-specific files."
    exit 0
fi

TOOL="$1"

echo "Syncing from: docs/ai-development-guide.md"
echo ""

if [ "$TOOL" = "all" ]; then
    sync_tool claude
    sync_tool cursor
    sync_tool copilot
    sync_tool gemini
    sync_tool windsurf
    echo ""
    echo "All tools synced."
else
    sync_tool "$TOOL"
fi

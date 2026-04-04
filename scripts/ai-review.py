#!/usr/bin/env python3
"""
AI Code Review Agent — Reviews code against project conventions.

Uses docs/ai-development-guide.md as the rulebook. Supports multiple
LLM providers (Anthropic, OpenAI, Google) — detects which API key
is available.

Usage:
    python scripts/ai-review.py              # review staged git changes
    python scripts/ai-review.py --all        # review entire project
    python scripts/ai-review.py file1 file2  # review specific files
    python scripts/ai-review.py --dry-run    # show what would be sent (no API call)

Security:
    - API key from environment variable only — never logged or printed
    - Warns before sending code to external API
    - Max file size: 100KB (skip large files)
    - Max total payload: 100K tokens
    - LLM output displayed as text — never executed
"""

import argparse
import os
import subprocess
import sys
from pathlib import Path

# ============================================================================
# Configuration
# ============================================================================

PROJECT_ROOT = Path(__file__).parent.parent
GUIDE_PATH = PROJECT_ROOT / "docs" / "ai-development-guide.md"
MAX_FILE_SIZE = 100 * 1024  # 100KB
MAX_TOTAL_CHARS = 400_000   # ~100K tokens
SOURCE_DIR = PROJECT_ROOT / "src" / "main" / "java"
TEST_DIR = PROJECT_ROOT / "src" / "test" / "java"
EXTENSIONS = {".java", ".yml", ".yaml", ".xml", ".properties"}


# ============================================================================
# Provider Detection
# ============================================================================

def detect_provider():
    """Detect which LLM provider is configured via environment variables."""
    if os.getenv("ANTHROPIC_API_KEY"):
        return "anthropic"
    if os.getenv("OPENAI_API_KEY"):
        return "openai"
    if os.getenv("GOOGLE_API_KEY"):
        return "google"
    return None


def get_provider_name(provider):
    """Human-readable provider name."""
    return {
        "anthropic": "Anthropic (Claude)",
        "openai": "OpenAI (GPT)",
        "google": "Google (Gemini)",
    }.get(provider, "Unknown")


# ============================================================================
# File Collection
# ============================================================================

def get_staged_files():
    """Get files staged for commit."""
    result = subprocess.run(
        ["git", "diff", "--cached", "--name-only", "--diff-filter=ACMR"],
        capture_output=True, text=True, cwd=PROJECT_ROOT
    )
    if result.returncode != 0:
        # No staged files — get modified files instead
        result = subprocess.run(
            ["git", "diff", "--name-only", "--diff-filter=ACMR"],
            capture_output=True, text=True, cwd=PROJECT_ROOT
        )
    files = [PROJECT_ROOT / f for f in result.stdout.strip().split("\n") if f]
    return [f for f in files if f.suffix in EXTENSIONS and f.exists()]


def get_all_source_files():
    """Get all source and test files."""
    files = []
    for directory in [SOURCE_DIR, TEST_DIR]:
        if directory.exists():
            for ext in EXTENSIONS:
                files.extend(directory.rglob(f"*{ext}"))
    # Also include config files
    for config in ["application.yml", "application-docker.yml", "logback-spring.xml"]:
        config_path = PROJECT_ROOT / "src" / "main" / "resources" / config
        if config_path.exists():
            files.append(config_path)
    return sorted(files)


def read_files(file_paths):
    """Read file contents, respecting size limits."""
    contents = []
    total_chars = 0

    for path in file_paths:
        if not path.exists():
            print(f"  ⚠️  Skipping (not found): {path}", file=sys.stderr)
            continue

        size = path.stat().st_size
        if size > MAX_FILE_SIZE:
            print(f"  ⚠️  Skipping (>{MAX_FILE_SIZE // 1024}KB): {path.name}", file=sys.stderr)
            continue

        content = path.read_text(encoding="utf-8", errors="replace")
        if total_chars + len(content) > MAX_TOTAL_CHARS:
            print(f"  ⚠️  Stopping (total size limit reached)", file=sys.stderr)
            break

        relative = path.relative_to(PROJECT_ROOT)
        contents.append(f"### {relative}\n```\n{content}\n```")
        total_chars += len(content)

    return "\n\n".join(contents), len(contents), total_chars


# ============================================================================
# LLM Calls (Provider-Agnostic)
# ============================================================================

DEFAULT_MODELS = {
    "anthropic": "claude-sonnet-4-20250514",
    "openai": "gpt-4o",
    "google": "gemini-1.5-pro",
}


def call_anthropic(system_prompt, user_prompt, model=None):
    """Call Anthropic Claude API."""
    import anthropic
    client = anthropic.Anthropic()
    response = client.messages.create(
        model=model or DEFAULT_MODELS["anthropic"],
        max_tokens=8192,
        system=system_prompt,
        messages=[{"role": "user", "content": user_prompt}],
    )
    return response.content[0].text


def call_openai(system_prompt, user_prompt, model=None):
    """Call OpenAI GPT API."""
    import openai
    client = openai.OpenAI()
    response = client.chat.completions.create(
        model=model or DEFAULT_MODELS["openai"],
        max_tokens=8192,
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ],
    )
    return response.choices[0].message.content


def call_google(system_prompt, user_prompt, model=None):
    """Call Google Gemini API."""
    import google.generativeai as genai
    genai.configure(api_key=os.getenv("GOOGLE_API_KEY"))
    gen_model = genai.GenerativeModel(model or DEFAULT_MODELS["google"])
    response = gen_model.generate_content(
        f"{system_prompt}\n\n{user_prompt}",
        generation_config=genai.GenerationConfig(max_output_tokens=8192),
    )
    return response.text


def call_llm(provider, system_prompt, user_prompt, model=None):
    """Route to the detected provider."""
    if provider == "anthropic":
        return call_anthropic(system_prompt, user_prompt, model)
    elif provider == "openai":
        return call_openai(system_prompt, user_prompt, model)
    elif provider == "google":
        return call_google(system_prompt, user_prompt, model)
    else:
        raise ValueError(f"Unknown provider: {provider}")


# ============================================================================
# Main
# ============================================================================

def main():
    parser = argparse.ArgumentParser(
        description="AI Code Review Agent — reviews code against project conventions"
    )
    parser.add_argument("files", nargs="*", help="Specific files to review")
    parser.add_argument("--all", action="store_true", help="Review entire project source")
    parser.add_argument("--dry-run", action="store_true", help="Show what would be sent (no API call)")
    parser.add_argument("--model", type=str, default=None, help="Override LLM model (e.g., claude-sonnet-4-20250514, claude-opus-4-20250514, gpt-4o)")
    args = parser.parse_args()

    # Detect provider
    provider = detect_provider()
    if not provider and not args.dry_run:
        print("ERROR: No LLM API key found.", file=sys.stderr)
        print("Set one of: ANTHROPIC_API_KEY, OPENAI_API_KEY, GOOGLE_API_KEY", file=sys.stderr)
        sys.exit(1)

    # Load conventions
    if not GUIDE_PATH.exists():
        print(f"ERROR: AI development guide not found: {GUIDE_PATH}", file=sys.stderr)
        sys.exit(1)
    conventions = GUIDE_PATH.read_text()

    # Collect files
    if args.files:
        file_paths = [Path(f) if Path(f).is_absolute() else PROJECT_ROOT / f for f in args.files]
    elif args.all:
        file_paths = get_all_source_files()
    else:
        file_paths = get_staged_files()
        if not file_paths:
            print("No staged or modified files found. Use --all to review entire project.")
            sys.exit(0)

    # Read files
    print(f"Collecting files...", file=sys.stderr)
    file_contents, file_count, total_chars = read_files(file_paths)

    if file_count == 0:
        print("No files to review.")
        sys.exit(0)

    print(f"  {file_count} files, {total_chars:,} characters", file=sys.stderr)

    # Dry run
    if args.dry_run:
        print(f"\n=== DRY RUN ===")
        print(f"Provider: {get_provider_name(provider) if provider else 'none (dry-run)'}")
        print(f"Files: {file_count}")
        print(f"Total size: {total_chars:,} chars (~{total_chars // 4:,} tokens)")
        print(f"Conventions: {len(conventions):,} chars")
        print(f"\nFiles that would be reviewed:")
        for p in file_paths:
            if p.exists() and p.stat().st_size <= MAX_FILE_SIZE:
                print(f"  {p.relative_to(PROJECT_ROOT)}")
        return

    # Security warning
    provider_name = get_provider_name(provider)
    print(f"\n⚠️  This will send {file_count} source files ({total_chars:,} chars) to {provider_name}.", file=sys.stderr)
    print(f"   Press Enter to continue or Ctrl+C to cancel...", file=sys.stderr)
    try:
        input()
    except KeyboardInterrupt:
        print("\nCancelled.")
        sys.exit(0)

    # Build prompts
    system_prompt = f"""You are a principal software engineer with 20+ years of experience conducting a formal code review for a production-grade Java service. You are an expert in Clean Architecture, SOLID principles, Java 21, Spring Boot, concurrency, security, and observability.

Your review must be thorough, specific, and actionable — the kind of feedback that would come from a senior architect at a top tech company. Reference specific file names and line numbers. Do not give generic advice.

## Project Conventions (MUST be enforced)

{conventions}

## Review Structure

Produce a structured review with these sections:

### 1. Executive Summary
A 2-3 sentence overall assessment. Rate the codebase: Production-Ready / Needs Minor Fixes / Needs Major Refactoring.

### 2. Architecture Compliance
- Does the domain layer have zero Spring imports?
- Do dependencies point inward only?
- Is ConverterConfig used for explicit wiring (no @Primary/@Qualifier)?
- Are use cases in the application layer, not controllers?
- Is the Port/Adapter pattern correctly applied?
For each violation, specify: file, line, what's wrong, how to fix.

### 3. Code Quality & SOLID Principles
- Single Responsibility: does each class have one reason to change?
- Open/Closed: can behavior be extended without modifying existing code?
- Naming: are classes, methods, variables self-documenting?
- Magic numbers/strings: are constants used appropriately?
- Readability: are methods small and focused?
For each issue, specify: file, what's wrong, suggested fix.

### 4. Concurrency & Thread Safety
- Is the ChunkedParallelExecutor correctly implemented?
- Are shared resources properly handled?
- Is the CachedConverter thread-safe?
- Are there potential race conditions?
- Is virtual thread usage appropriate?

### 5. Security Review
- Input validation: are all inputs validated at boundaries?
- XSS prevention: is user input sanitized in error messages?
- Error handling: are stack traces ever exposed to clients?
- API key handling: are keys hardcoded anywhere?
- Rate limiting: is the implementation correct?
- Filter ordering: is the security filter chain correct?

### 6. Testing Assessment
- Are there missing test cases or edge cases?
- Is the testing pyramid balanced (unit > integration > E2E)?
- Are tests testing behavior or implementation details?
- Thread safety tests: are they adequate?
- Are test assertions specific enough?

### 7. Observability Review
- Are all business metrics recorded?
- Is structured logging consistent?
- Is correlation ID properly propagated and cleaned up?
- Are health checks meaningful?

### 8. What's Excellent
Highlight 3-5 things that are done exceptionally well. Be specific about WHY they're good — not just "good naming" but "the sealed exception hierarchy in DomainException.java enables exhaustive handling in GlobalExceptionHandler."

### 9. Priority Improvements
List the top 5 improvements ranked by impact. For each:
- **What**: specific change needed
- **Where**: file and location
- **Why**: what risk or issue it addresses
- **How**: concrete code suggestion

### 10. Overall Score
Rate each area out of 10:
- Architecture: X/10
- Code Quality: X/10
- Security: X/10
- Testing: X/10
- Observability: X/10
- Overall: X/10"""

    user_prompt = f"Conduct a formal principal-engineer-level code review of the following production Java service:\n\n{file_contents}"

    # Call LLM
    model_name = args.model or DEFAULT_MODELS.get(provider, "default")
    print(f"\nSending to {provider_name} (model: {model_name})...", file=sys.stderr)
    try:
        review = call_llm(provider, system_prompt, user_prompt, args.model)
        print(f"\n{'=' * 60}")
        print(f"  AI CODE REVIEW ({provider_name} — {model_name})")
        print(f"{'=' * 60}\n")
        print(review)
        print(f"\n{'=' * 60}")
    except Exception as e:
        print(f"\nERROR: API call failed: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()

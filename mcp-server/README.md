# Roman Numeral MCP Server

MCP (Model Context Protocol) server that exposes the Roman Numeral Conversion API as AI-callable tools. Any MCP-compatible AI agent (Claude Desktop, etc.) can use the conversion service as a tool.

## How It Works

```
AI Agent (Claude, GPT, etc.)
  ↓ MCP protocol
MCP Server (this Python script)
  ↓ HTTP GET
Roman Numeral Service (localhost:8080)
  ↓
Response back to AI Agent
```

This is a **thin HTTP client** — it calls the REST API and returns the result. No business logic, no shared code with the Java service.

## Available Tools

| Tool | Description | Example |
|------|-------------|---------|
| `convert_number` | Convert a single integer (1-3999) to Roman numeral | `convert_number(1994)` → `MCMXCIV` |
| `convert_range` | Convert a range to Roman numerals (parallel) | `convert_range(1, 10)` → 10 conversions |
| `check_health` | Check service health status | `check_health()` → UP |

## Setup

### Prerequisites

- Python 3.11+
- Roman numeral service running (`localhost:8080`)

### Install

```bash
cd mcp-server
pip install -r requirements.txt

# Copy and configure environment
cp .env.example .env
# Edit .env if service is not on localhost:8080
```

### Connect to Claude Desktop

Add to your Claude Desktop config (`~/Library/Application Support/Claude/claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "roman-numeral": {
      "command": "python",
      "args": ["/path/to/roman-numeral-service/mcp-server/server.py"],
      "env": {
        "SERVICE_URL": "http://localhost:8080",
        "API_KEY": "test-api-key-1"
      }
    }
  }
}
```

Then restart Claude Desktop. You can now ask Claude:
- "Convert 1994 to a Roman numeral"
- "Convert all numbers from 1 to 20 to Roman numerals"
- "Check if the Roman numeral service is healthy"

## Security

- API key read from environment variable — never hardcoded
- `.env` file is gitignored
- Input validated before sending to API (type + range checks)
- HTTP timeout: 5 seconds (no hanging connections)
- Error responses are clean JSON — no stack traces exposed
- No `eval()`, `exec()`, or file system access — HTTP GET only

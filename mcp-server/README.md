# Roman Numeral MCP Server

MCP (Model Context Protocol) server that exposes the Roman Numeral Conversion API as AI-callable tools. Any MCP-compatible AI agent can use the conversion service as a tool.

## How It Works

```
AI Agent (Claude Desktop, Claude Code, Cursor, etc.)
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
| `convert_number` | Convert a single integer (1-3999) to Roman numeral | "Convert 1994 to Roman numeral" → `MCMXCIV` |
| `convert_range` | Convert a range to Roman numerals (parallel) | "Convert numbers 1 to 20" → 20 conversions |

## Setup

### Prerequisites

- Python 3.11+
- Roman numeral service running on `localhost:8080`

### Install

```bash
cd mcp-server
pip install -r requirements.txt
```

## Connect to Your AI Tool

**Step 1:** Start the Roman numeral service
```bash
# From the project root
./mvnw spring-boot:run
```

**Step 2:** Connect the MCP server to your AI tool (pick one):

### Claude Desktop

Add to `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "roman-numeral": {
      "command": "python3",
      "args": ["<full-path-to-project>/mcp-server/server.py"],
      "env": {
        "SERVICE_URL": "http://localhost:8080",
        "API_KEY": "test-api-key-1"
      }
    }
  }
}
```

Restart Claude Desktop. Ask: *"Convert 1994 to a Roman numeral"*

### Claude Code (CLI)

```bash
claude mcp add roman-numeral -- python3 mcp-server/server.py
```

Then ask Claude Code: *"Convert 1994 to a Roman numeral"*

### Cursor

Add to `.cursor/mcp.json` in the project root:

```json
{
  "mcpServers": {
    "roman-numeral": {
      "command": "python3",
      "args": ["mcp-server/server.py"],
      "env": {
        "SERVICE_URL": "http://localhost:8080",
        "API_KEY": "test-api-key-1"
      }
    }
  }
}
```

Restart Cursor. The tools appear in Cursor's MCP tool list.

### VS Code (GitHub Copilot with MCP support)

Add to `.vscode/mcp.json`:

```json
{
  "servers": {
    "roman-numeral": {
      "command": "python3",
      "args": ["mcp-server/server.py"],
      "env": {
        "SERVICE_URL": "http://localhost:8080",
        "API_KEY": "test-api-key-1"
      }
    }
  }
}
```

### Any MCP-Compatible Client

The server uses stdio transport (standard MCP). Run:

```bash
SERVICE_URL=http://localhost:8080 API_KEY=test-api-key-1 python3 mcp-server/server.py
```

Connect your MCP client to stdin/stdout of this process.

## Test It

Once connected, ask your AI agent:
- *"Convert 1994 to a Roman numeral"*
- *"What is 42 in Roman numerals?"*
- *"Convert all numbers from 1 to 20 to Roman numerals"*
- *"What are the Roman numerals for 3998 and 3999?"*

## Security

- API key from environment variable — never hardcoded
- `.env` file is gitignored
- Input validated before sending to API (type + range checks)
- HTTP timeout: 5 seconds (no hanging connections)
- Error responses are clean JSON — no stack traces exposed
- No `eval()`, `exec()`, or file system access — HTTP GET only

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
python3 -m venv venv
source venv/bin/activate    # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

## Connect to Your AI Tool

**Step 1:** Make sure the Roman numeral service is running (via `./mvnw spring-boot:run` or `docker compose up`)

**Step 2:** Connect the MCP server to your AI tool (pick one):

### Claude Code (CLI)

> **Important:** Run `claude mcp add` from the **project root** (not `mcp-server/`). Use **full absolute paths** for both the venv python and server.py.

```bash
# From the project root: /path/to/roman-numeral-service/
claude mcp add roman-numeral \
  -e SERVICE_URL=http://localhost:8080 \
  -e API_KEY=test-api-key-1 \
  -- /absolute/path/to/roman-numeral-service/mcp-server/venv/bin/python3 \
  /absolute/path/to/roman-numeral-service/mcp-server/server.py

# Start a new Claude Code session from the project root
claude
```

Then ask: *"Convert 1994 to a Roman numeral"*

To remove: `claude mcp remove roman-numeral`

### Claude Desktop

Add to `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "roman-numeral": {
      "command": "/absolute/path/to/roman-numeral-service/mcp-server/venv/bin/python3",
      "args": ["/absolute/path/to/roman-numeral-service/mcp-server/server.py"],
      "env": {
        "SERVICE_URL": "http://localhost:8080",
        "API_KEY": "test-api-key-1"
      }
    }
  }
}
```

Restart Claude Desktop. Ask: *"Convert 1994 to a Roman numeral"*

### Other MCP-Compatible Tools (Cursor, VS Code, Windsurf, etc.)

This server uses **stdio transport** (standard MCP protocol). To connect any MCP-compatible tool, configure it to run:

```
Command:  /absolute/path/to/roman-numeral-service/mcp-server/venv/bin/python3
Args:     /absolute/path/to/roman-numeral-service/mcp-server/server.py
Env vars: SERVICE_URL=http://localhost:8080
          API_KEY=test-api-key-1
```

Refer to your tool's MCP documentation for the exact config format:
- **Cursor**: [Cursor MCP docs](https://docs.cursor.com/context/model-context-protocol)
- **VS Code / Copilot**: [VS Code MCP docs](https://code.visualstudio.com/docs/copilot/chat/mcp-servers)
- **Windsurf**: Check Windsurf's MCP configuration guide

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

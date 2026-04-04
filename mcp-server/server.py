"""
Roman Numeral MCP Server — AI-callable tools for the conversion service.

Exposes the Roman Numeral REST API as MCP tools so any AI agent
(Claude, GPT, etc.) can use the conversion service as a tool.

Thin HTTP client — calls the REST API, does not embed business logic.

Usage:
    python server.py                          # default: localhost:8080
    SERVICE_URL=http://host:8080 python server.py
"""

import json
import os
import sys
from typing import Any

import httpx
from mcp.server import Server
from mcp.server.stdio import run_server
from mcp.types import TextContent, Tool

# Configuration
SERVICE_URL = os.getenv("SERVICE_URL", "http://localhost:8080")
API_KEY = os.getenv("API_KEY", "test-api-key-1")
HTTP_TIMEOUT = 5.0  # seconds — prevent hanging connections

# MCP Server
server = Server("roman-numeral-service")

# HTTP client with timeout and API key
http_client = httpx.Client(
    base_url=SERVICE_URL,
    headers={"X-API-Key": API_KEY},
    timeout=HTTP_TIMEOUT,
)


def _safe_request(url: str, params: dict) -> dict:
    """Make an HTTP request with error handling. Never exposes stack traces."""
    try:
        response = http_client.get(url, params=params)
        return {"status": response.status_code, "body": response.json()}
    except httpx.TimeoutException:
        return {"status": 408, "body": {"error": "Timeout", "message": f"Service did not respond within {HTTP_TIMEOUT}s"}}
    except httpx.ConnectError:
        return {"status": 503, "body": {"error": "Unavailable", "message": f"Cannot connect to {SERVICE_URL}"}}
    except Exception as e:
        return {"status": 500, "body": {"error": "Error", "message": str(e)}}


@server.list_tools()
async def list_tools() -> list[Tool]:
    """Register available tools."""
    return [
        Tool(
            name="convert_number",
            description="Convert a single integer (1-3999) to its Roman numeral representation.",
            inputSchema={
                "type": "object",
                "properties": {
                    "number": {
                        "type": "integer",
                        "description": "Integer to convert (1-3999)",
                        "minimum": 1,
                        "maximum": 3999,
                    }
                },
                "required": ["number"],
            },
        ),
        Tool(
            name="convert_range",
            description="Convert a range of integers to Roman numerals using parallel computation. Returns all conversions in ascending order.",
            inputSchema={
                "type": "object",
                "properties": {
                    "min": {
                        "type": "integer",
                        "description": "Range minimum (1-3999)",
                        "minimum": 1,
                        "maximum": 3999,
                    },
                    "max": {
                        "type": "integer",
                        "description": "Range maximum (1-3999, must be greater than min)",
                        "minimum": 1,
                        "maximum": 3999,
                    },
                },
                "required": ["min", "max"],
            },
        ),
    ]


@server.call_tool()
async def call_tool(name: str, arguments: dict[str, Any]) -> list[TextContent]:
    """Handle tool calls."""

    if name == "convert_number":
        number = arguments.get("number")
        # Input validation — don't send bad data to the API
        if not isinstance(number, int) or number < 1 or number > 3999:
            return [TextContent(type="text", text=json.dumps({
                "error": "Invalid input",
                "message": "Number must be an integer between 1 and 3999",
            }))]

        result = _safe_request("/romannumeral", {"query": number})
        return [TextContent(type="text", text=json.dumps(result["body"], indent=2))]

    elif name == "convert_range":
        min_val = arguments.get("min")
        max_val = arguments.get("max")
        # Input validation
        if not isinstance(min_val, int) or not isinstance(max_val, int):
            return [TextContent(type="text", text=json.dumps({
                "error": "Invalid input",
                "message": "min and max must be integers",
            }))]
        if min_val < 1 or max_val > 3999 or min_val >= max_val:
            return [TextContent(type="text", text=json.dumps({
                "error": "Invalid range",
                "message": "min must be < max, both between 1 and 3999",
            }))]

        result = _safe_request("/romannumeral", {"min": min_val, "max": max_val})
        return [TextContent(type="text", text=json.dumps(result["body"], indent=2))]

    else:
        return [TextContent(type="text", text=json.dumps({
            "error": "Unknown tool",
            "message": f"Tool '{name}' is not available",
        }))]


if __name__ == "__main__":
    import asyncio
    print(f"Starting Roman Numeral MCP Server (service: {SERVICE_URL})", file=sys.stderr)
    asyncio.run(run_server(server))

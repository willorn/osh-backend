# Agent Instructions

This file defines shared rules for AI agents in this repository.

## ZenTao MCP token guard (mandatory)

Before any operation that uses `mcpServers.zentao` in `MCP.json`, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\bin\ensure_zentao_mcp_token.ps1 -ConfigPath .\MCP.json
```

Rules:

1. If token exists and is valid, continue MCP operation.
2. If token is empty or expired, request account/password and refresh token first.
3. Do not skip this step for ZenTao MCP calls.

## Auto workflow for ZenTao MCP

1. Always execute token guard command first.
2. If guard returns valid token, continue ZenTao MCP operation directly.
3. If guard asks for login, prompt user for account/password and complete refresh.
4. Only after successful refresh and verification, continue ZenTao MCP operation.


# Copilot Instructions

Shared policy source: `AGENTS.md` at repository root. Keep this file aligned with that source.

## ZenTao MCP token guard

Before any operation that uses `mcpServers.zentao` from `MCP.json`, run the token guard script first:

```powershell
powershell -ExecutionPolicy Bypass -File .\bin\ensure_zentao_mcp_token.ps1 -ConfigPath .\MCP.json
```

Rules:

- If token exists and is valid, continue MCP operation.
- If token is empty or expired, request account/password and refresh token first.
- Do not skip this step for ZenTao MCP calls.

## Auto workflow for MCP calls

When the current task needs ZenTao MCP (`mcpServers.zentao`):

1. Always execute token guard command first.
2. If guard returns valid token, continue ZenTao MCP operation directly.
3. If guard asks for login, prompt user for account/password and complete refresh.
4. Only after successful refresh+verification, continue ZenTao MCP operation.

## Cross-agent consistency

The same workflow is mirrored in:

- `AGENTS.md`
- `.cursorrules`
- `CLAUDE.md`
- `GEMINI.md`


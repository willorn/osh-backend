# zentao-mcp-token-guard skill

## Purpose

Ensure `mcpServers.zentao.headers.token` in `MCP.json` is always usable before any ZenTao MCP operation.

## Auto Trigger Rule

Use this skill automatically whenever a task plans to access ZenTao through `mcpServers.zentao` in `MCP.json`.
No ZenTao MCP read/write/list/update action should start before this guard completes.

## Behavior

1. Read `MCP.json` and check `mcpServers.zentao.headers.token`.
2. Validate token by calling `GET /api.php/v2/users?page=1&limit=1`.
3. If token exists and is valid, continue using it.
4. If token is empty or expired, prompt for account/password.
5. Call `POST /api.php/v2/users/login` to get a new token.
6. Write the token back to `MCP.json`.
7. Verify new token and continue MCP operations.

## Command

```powershell
powershell -ExecutionPolicy Bypass -File .\bin\ensure_zentao_mcp_token.ps1 -ConfigPath .\MCP.json
```

## Force re-login

```powershell
powershell -ExecutionPolicy Bypass -File .\bin\ensure_zentao_mcp_token.ps1 -ConfigPath .\MCP.json -ForceLogin
```

## Notes

- The script prefers `mcpServers.zentao.apiBaseUrl` when present, and falls back to MCP URL-derived API base candidates.
- If login response does not expose token in JSON, the script falls back to parse `zentaosid` from `Set-Cookie`.
- Keep `MCP.json` protected because it stores runtime token.
- Keep cross-agent instructions in sync across `AGENTS.md`, `.github/copilot-instructions.md`, `.cursorrules`, `CLAUDE.md`, and `GEMINI.md`.


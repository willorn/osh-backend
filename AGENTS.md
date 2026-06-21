# Agent Instructions

This file defines shared rules for AI agents in this repository.

## Mandatory SDD / OpenSpec workflow

This repository uses SDD + OpenSpec + harness discipline for all AI-assisted development.

Before code changes, every agent must read:

1. `docs/ai/README.md`
2. `docs/ai/02-workflow.md`
3. `openspec/project.md`
4. The active change under `openspec/changes/<change-id>/`, unless the task is explicitly OpenSpec-exempt.

Rules:

- Do not start medium or large code changes without an OpenSpec change.
- Do not change API contracts, permissions, data models, middleware configuration, deployment behavior, or storage behavior without `proposal.md`, `design.md`, `tasks.md`, and `specs/**/spec.md`.
- Preserve user changes. Never discard local edits unless the user explicitly asks.
- Keep edits scoped to the current task.
- Run relevant verification and report exact results.
- If skipping OpenSpec for a tiny change, the PR must include `OpenSpec-Exempt: true` and a clear reason.

Recommended Codex prompt:

```text
请先阅读 AGENTS.md、docs/ai/README.md、openspec/project.md，以及 openspec/changes/<change-id>/ 下的 proposal/design/tasks/spec。
按 tasks.md 小步实现，不做无关重构。
完成后运行验证并汇报。
```

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

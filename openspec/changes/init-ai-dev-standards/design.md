# Design: init-ai-dev-standards

## 方案概览

本次只新增工程流程文件和 CI 检查，不触碰业务代码。通过仓库内文档、模板、Agent 指令和 GitHub Actions，把“先规格、再实现、再审查”的流程固化下来。

## 现状分析

- 相关模块：仓库根目录、`.github/`、`docs/ai/`、`openspec/`、`scripts/`。
- 相关类/接口：无。
- 当前数据结构：无变更。
- 当前配置：扩展 PR check workflow。

## 详细设计

### 文件设计

| 路径 | 作用 |
|---|---|
| `openspec/project.md` | 项目级 SDD/OpenSpec 总说明 |
| `openspec/templates/*` | change 模板 |
| `openspec/specs/baseline/spec.md` | 当前行为基线 |
| `docs/ai/newcomer-guide.md` | 小白上手教程 |
| `docs/ai/conventions/*` | 技术约定 |
| `AGENTS.md` | Codex/AI Agent 根规则 |
| `scripts/verify-ai-process.sh` | CI 流程检查 |
| `.github/PULL_REQUEST_TEMPLATE.md` | PR 强制说明 |

### CI 设计

`pr-check.yml` 新增 `process-guard` job：

- checkout 全量历史。
- 读取 PR body 判断是否有 `OpenSpec-Exempt: true` 和 `Reason:`。
- 执行 `scripts/verify-ai-process.sh`。
- 如果没有 OpenSpec change，也没有豁免说明，则失败。

### 接口设计

无运行时接口变更。

### 数据设计

无数据库变更。

### 中间件影响

- Redis：无。
- Kafka：无。
- Elasticsearch：无。
- Nacos：无。
- R2/S3：无。

## 兼容性

- 不影响后端运行。
- 不影响部署产物。
- 只影响 PR 检查流程。

## 测试计划

- 本地执行 `ALLOW_OPENSPEC_EXEMPT=true bash scripts/verify-ai-process.sh`。
- 本分支包含实际 OpenSpec change 后，执行 `bash scripts/verify-ai-process.sh`。

## 发布与回滚

- 发布步骤：通过 PR 合并到目标分支。
- 验证命令：`bash scripts/verify-ai-process.sh`。
- 回滚步骤：revert 本次 commit。

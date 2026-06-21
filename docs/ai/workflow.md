# AI 开发工作流

## 标准流程

```text
需求进入
  -> 判断是否需要 OpenSpec
  -> 创建 openspec/changes/<change-id>
  -> proposal/design/spec/tasks 评审
  -> 新建 feature 分支
  -> Codex 按 tasks 实现
  -> 本地验证
  -> PR
  -> Spec & Process Guard
  -> Reviewer 按 CODE_REVIEW.md 审查
  -> 合并到 QA / release
```

## 分支规则

- 功能分支：`feature/<module>_<owner>` 或 `codex/<change-id>`。
- 修复分支：`fix/<change-id>`。
- 规范分支：`codex/<topic>`。
- 禁止直接在 `qa/**`、`release/**` 上做大改。

## Codex 执行协议

每次让 Codex 开发时，必须让它先读：

1. `AGENTS.md`
2. `openspec/project.md`
3. 当前 change 的 `proposal.md`
4. 当前 change 的 `design.md`
5. 当前 change 的 `tasks.md`
6. 相关 `docs/ai/conventions/*.md`

Codex 必须：

- 先说明理解和计划。
- 只改本任务相关文件。
- 不覆盖未授权本地改动。
- 不提交密钥、token、日志和构建产物。
- 完成后运行验证。
- 最终汇报修改文件、验证结果和风险。

## 轻量豁免

如果不建 OpenSpec，PR 描述必须包含：

```text
OpenSpec-Exempt: true
Reason: <为什么不需要规格>
```

CI 会检查这个字段。没有 change，也没有豁免说明，PR 会失败。

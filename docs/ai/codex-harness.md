# Codex Harness 使用规范

## Codex 在本仓库的角色

Codex 是执行者、分析者和 Reviewer 辅助者，不是需求源头。需求源头必须是 OpenSpec、禅道任务、PR 描述或负责人明确说明。

## 每次开发前

Codex 必须确认：

- 当前分支。
- 工作区是否有未提交改动。
- 是否需要保护本地改动。
- 是否存在对应 OpenSpec change。
- 任务是否涉及接口、权限、数据、中间件、部署。

## 推荐提示词

```text
你在 osh-backend 仓库工作。
请先阅读 AGENTS.md、openspec/project.md、openspec/changes/<change-id>/proposal.md、design.md、tasks.md。
基于当前 qa 分支新建 codex/<change-id> 分支。
只实现 tasks.md 中的任务，不做无关重构。
完成后运行 mvn clean install -DskipTests -pl '!backstage-flink'，并汇报结果。
```

## 禁止事项

- 禁止没有说明就改公共接口。
- 禁止没有说明就改权限判断。
- 禁止没有说明就改数据库字段含义。
- 禁止把 token、密码、私钥写入仓库。
- 禁止用 `git reset --hard`、`git checkout -- .` 清理别人改动。
- 禁止为了让 CI 过而删除测试。

## 禅道 MCP

如果任务涉及禅道 MCP，必须遵守根目录 `AGENTS.md` 的 token guard 规则。

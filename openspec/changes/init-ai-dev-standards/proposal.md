# Proposal: init-ai-dev-standards

## 背景

团队正在引入 SDD、OpenSpec、AI harness 和 Superpowers 风格的开发纪律，用来统一 Codex 和团队成员的开发模式、代码风格、代码审核、升级规划和新人上手路径。

当前问题：

- 需求和实现之间缺少统一规格入口。
- Codex 开发缺少仓库内硬约束。
- PR 审查标准分散。
- 新人需要靠问人理解开发流程。
- 后续 JDK、Spring Boot、Redis、Kafka、ES、Nacos、R2/S3、微服务拆分等升级需要更强流程约束。

## 目标

- 建立 `openspec/` 目录和变更模板。
- 建立 `docs/ai/` 新人教程、工作流、代码风格、测试、审查、发布和升级文档。
- 建立根目录 `AGENTS.md`、`CONTRIBUTING.md`、`CODE_REVIEW.md`、`SECURITY.md`。
- 建立 PR/Issue 模板。
- 建立 `Spec & Process Guard`，让 PR 自动检查 OpenSpec 和流程文件。

## 非目标

- 本次不修改业务代码。
- 本次不升级依赖版本。
- 本次不改部署逻辑。
- 本次不拆分微服务。

## 用户影响

运行时无直接用户影响。开发流程会改变：

- 中大型需求需要先写 OpenSpec change。
- PR 会自动触发流程检查。
- 新人从 `docs/ai/01-newcomer-guide.md` 开始上手。

## 风险

- 兼容性风险：无运行时兼容风险。
- 数据风险：无数据变更。
- 性能风险：无运行时性能影响。
- 安全风险：文档中明确禁止提交敏感信息。
- 回滚风险：可通过 revert 本次文档和 CI 变更回滚。

## 验收标准

- [ ] `openspec/project.md` 和模板存在。
- [ ] `docs/ai/01-newcomer-guide.md` 足够新人理解概念和流程。
- [ ] PR 模板包含 OpenSpec 和风险回滚检查。
- [ ] CI 包含 `Spec & Process Guard`。
- [ ] 验证脚本本地可运行。

## 关联信息

- OpenSpec 参考：https://github.com/Fission-AI/OpenSpec
- Superpowers 参考：https://github.com/obra/superpowers

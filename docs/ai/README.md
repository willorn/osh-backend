# AI 开发规范入口

这套目录是 `osh-backend` 的 AI Coding 工作方式。目标不是多写文档，而是让每个人、每个 AI Agent、每个 PR 都按同一套节奏工作。

## 必读顺序

1. [小白上手教程](newcomer-guide.md)
2. [AI 开发工作流](workflow.md)
3. [Codex Harness 使用规范](codex-harness.md)
4. [架构说明](architecture.md)
5. [代码风格](code-style.md)
6. [测试规范](testing.md)
7. [代码审核标准](review-rubric.md)
8. [发布流程](release-flow.md)
9. [升级策略](upgrade-strategy.md)

## 核心约束

- 中大型需求必须先建 `openspec/changes/<change-id>/`。
- Codex 开发必须先读 `AGENTS.md` 和当前 change 的 `proposal/design/tasks/spec`。
- PR 必须经过 GitHub Actions 的 `Spec & Process Guard`。
- 任何人不得绕过 OpenSpec，把需求只写在聊天记录里。
- 紧急修复可以先发，但必须在事后补 OpenSpec change。

## 当前仓库属性

- 类型：后端 Spring Boot 多模块服务。
- 当前基线：Java 8 运行语义，CI 使用 JDK 17 构建。
- 演进方向：组件升级、模块边界收敛、最终按业务能力拆微服务。

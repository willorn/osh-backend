# 新人上手教程：用 SDD + Harness + OpenSpec + Superpowers 开发 OSH Backend

这份文档给第一次加入项目的人看，也给第一次让 Codex 接手需求的人看。你不需要先成为架构师，但你必须知道团队怎么把一个想法变成稳定代码。

## 1. 先理解这几个概念

### 1.1 SDD 是什么

SDD 是 Spec Driven Development，规格驱动开发。

它的意思是：不要一上来就改代码，而是先写清楚系统应该表现成什么样。

传统开发里经常发生这种事：

- 产品或负责人说一句“这里优化一下”。
- 开发按自己的理解改了。
- 测试发现理解不一样。
- 上线后用户路径被破坏。
- 后来新人完全不知道当初为什么这么写。

SDD 要解决的就是这个混乱。它要求每个重要变更都有一份可追踪规格：

- 为什么做。
- 做什么。
- 不做什么。
- 哪些接口、数据、权限、页面会受影响。
- 怎么验收。
- 怎么回滚。

### 1.2 OpenSpec 是什么

OpenSpec 是一种把需求变更结构化的方式。它通常把一个变更放到独立目录：

```text
openspec/changes/<change-id>/
  proposal.md
  design.md
  tasks.md
  specs/
    <capability>/
      spec.md
```

在本项目里，OpenSpec 是团队的“需求事实来源”。如果 Codex 对需求理解不清，先看 OpenSpec；如果 Reviewer 不知道为什么这么改，也先看 OpenSpec。

### 1.3 Harness 是什么

Harness 在这里不是某一个固定产品，而是 AI Coding 的运行承载环境。Codex、Claude Code、Cursor、Copilot Chat 都可以看成不同 harness。

Harness 的职责是：

- 读取仓库里的规则。
- 按团队流程执行。
- 调用终端、编辑文件、运行测试。
- 把结果反馈给人。

所以我们要写 `AGENTS.md`、`.github/copilot-instructions.md`、`docs/ai/03-codex-harness.md`，让不同 AI 工具都知道团队规矩。

### 1.4 Superpowers 是什么

Superpowers 是一种更强约束的 AI 开发流程思路。它强调：

- 先理解任务。
- 先建计划。
- 在独立分支或 worktree 工作。
- 小步实现。
- 运行测试。
- 做代码审查。
- 完成后总结风险。

我们不会照搬某个开源项目的所有细节，但会吸收它的思想：让 AI 像一个靠谱工程师，而不是一个兴奋地乱改代码的自动补全器。

### 1.5 为什么这套东西对 OSH 很重要

OSH 现在是 Vue/Nuxt 前端 + Spring Boot 后端 + Redis/ES/Nacos/R2/支付等中间件。后面还会升级 JDK、Spring Boot、Redis、Kafka、ES，并逐步拆微服务。

如果没有统一流程，会出现：

- 每个人写法不同。
- 接口随手改，前端突然炸。
- 权限逻辑散落各处。
- 中间件升级没人知道影响面。
- 新人只能靠问人和猜。
- Codex 每次都从零读代码，浪费时间还容易误伤。

这套规范就是给团队铺一条轨道。轨道不是束缚创造力，是避免大家在泥地里推车。

## 2. 仓库结构先看哪里

后端是 Maven 多模块项目，核心目录：

```text
backstage-admin/       启动入口、后台 Controller、应用装配
backstage-common/      通用工具、基础对象、公共能力
backstage-framework/   Web、安全、Redis、拦截器、框架配置
backstage-system/      主要业务模块，包含课程、工具、订单、用户、反馈等
backstage-quartz/      定时任务
backstage-generator/   代码生成
backstage-flink/       Flink 相关能力
backstage-hbase/       HBase 相关能力
openspec/              规格驱动开发目录
docs/ai/               AI 开发规范和新人教程
```

新人优先看：

1. `docs/ai/README.md`
2. `openspec/project.md`
3. `docs/ai/04-architecture.md`
4. `docs/ai/conventions/springboot.md`
5. `docs/ai/conventions/api.md`
6. `CODE_REVIEW.md`

## 3. 新人接一个需求时怎么做

### 第一步：确认任务类型

先判断是不是中大型变更。

必须建 OpenSpec change 的情况：

- 新功能。
- 影响用户行为的 Bug 修复。
- 改接口请求或响应。
- 改权限、登录、课程权益、支付、订单。
- 改数据库结构。
- 改 Redis、Kafka、ES、Nacos、R2/S3。
- 升级 JDK、Spring Boot、Spring Cloud 等基础组件。
- 拆服务、拆模块、改架构边界。

可以轻量豁免的情况：

- 文档错别字。
- 注释修正。
- 不影响行为的小格式调整。

### 第二步：创建 change

复制模板：

```bash
mkdir -p openspec/changes/<change-id>/specs/<capability>
cp openspec/templates/proposal.md openspec/changes/<change-id>/proposal.md
cp openspec/templates/design.md openspec/changes/<change-id>/design.md
cp openspec/templates/tasks.md openspec/changes/<change-id>/tasks.md
cp openspec/templates/spec.md openspec/changes/<change-id>/specs/<capability>/spec.md
```

命名示例：

```text
fix-course-access-for-founder
add-course-hide-flag
upgrade-springboot-3
split-order-service
```

### 第三步：写 proposal

`proposal.md` 要让不看代码的人也能懂。

必须回答：

- 现在有什么问题？
- 为什么必须做？
- 做完对用户有什么影响？
- 哪些事情本次不做？
- 什么结果算完成？

### 第四步：写 design

`design.md` 写给开发和 Reviewer 看。

必须写：

- 涉及哪些模块、类、接口。
- 是否改数据库。
- 是否改缓存。
- 是否改 ES 索引。
- 是否改配置。
- 是否需要前端同步。
- 如何测试。
- 如何回滚。

### 第五步：写 spec

`spec.md` 写行为验收。

推荐格式：

```text
Requirement: 已购买小班权益的用户可以观看课程

GIVEN 用户已经购买小班权益
WHEN 用户打开课程小节
THEN 后端返回允许观看
```

好 spec 的标准是：测试和开发能直接照着验收。

### 第六步：让 Codex 开发

给 Codex 的提示应该包含：

```text
请先阅读 AGENTS.md、openspec/project.md、openspec/changes/<change-id>/ 下的 proposal/design/tasks/spec。
基于当前分支新建 feature 分支。
只实现 tasks.md 里的内容。
不要改无关文件。
完成后运行测试并给出结果。
```

不要只说“帮我改一下权限”。这太模糊了，AI 会想象一座宫殿，有时宫殿还会建在别人家地里。

## 4. 本地开发基本命令

构建后端：

```bash
mvn clean install -DskipTests -pl '!backstage-flink'
```

运行测试：

```bash
mvn test -pl backstage-system
```

只看变更：

```bash
git status --short
git diff
git diff --cached
```

## 5. 提 PR 前必须检查

PR 前确认：

- [ ] 有 OpenSpec change，或者 PR 写明 `OpenSpec-Exempt: true` 和原因。
- [ ] `tasks.md` 全部勾选或解释未完成项。
- [ ] 接口兼容性已说明。
- [ ] 权限影响已说明。
- [ ] 数据和缓存影响已说明。
- [ ] 本地构建或测试已运行。
- [ ] 没有提交密码、token、密钥。
- [ ] 没有把临时文件、日志、编译产物提交上来。

## 6. Reviewer 怎么审

Reviewer 不只看代码好不好看，要看：

- 是否满足 spec。
- 有没有遗漏异常分支。
- 有没有破坏已有接口。
- 权限是否绕过。
- 事务边界是否正确。
- 缓存是否一致。
- ES/Redis/Kafka 是否有兼容问题。
- 日志是否足够排查。
- 回滚是否可操作。

## 7. 以后升级和微服务怎么走

升级 JDK、Spring Boot、Redis、Kafka、ES 这类事情，不能靠一个“大哥试试看”。

必须先写：

- 兼容性矩阵。
- 影响模块清单。
- 本地验证方式。
- QA 环境验证方式。
- 灰度计划。
- 回滚计划。

拆微服务时，要按业务能力拆，不按 Controller 文件夹随便拆。候选边界包括：

- 用户与权限。
- 课程与学习权益。
- 订单与支付。
- 工具与使用次数。
- 反馈与问答。
- 内容与搜索。

每次拆分都必须保证：

- 老接口兼容。
- 数据迁移可回滚。
- 服务间调用可观测。
- 限流、超时、重试、幂等都明确。

## 8. 出问题时怎么办

如果线上出问题：

1. 先止血，不争论。
2. 记录现象、时间、影响范围。
3. 找到关联 PR 和 OpenSpec change。
4. 按 release-flow 回滚或热修。
5. 事后补充 spec、测试和复盘。

没有记录的问题，会变成下一次的坑。记录不是为了追责，是为了让系统记住疼痛。

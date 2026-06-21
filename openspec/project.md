# OSH Backend OpenSpec Project

## 项目定位

`osh-backend` 是 OSH 平台后端服务，当前以 Spring Boot 单体多模块为主，承载主网站、课程、工具、订单、拼团、秒杀、反馈、问答、用户与后台管理等能力。后续会逐步升级 JDK、Spring Boot、Spring Cloud、Redis、Kafka、Elasticsearch、Nacos 等基础组件，并按业务边界演进到微服务。

## SDD 工作原则

SDD 是 Spec Driven Development，意思是先把行为和验收标准写清楚，再写代码。任何会影响用户行为、接口契约、数据结构、权限、配置、部署或中间件的变更，都必须先建 `openspec/changes/<change-id>/`。

小修小补可以走轻量流程，但必须在 PR 里写清楚豁免理由。禁止把需求只留在聊天记录、微信群、口头说明或 Codex 对话里。

## OpenSpec 目录约定

```text
openspec/
  project.md
  specs/
    baseline/
      spec.md
  changes/
    README.md
    <change-id>/
      proposal.md
      design.md
      tasks.md
      specs/
        <capability>/
          spec.md
  templates/
    proposal.md
    design.md
    tasks.md
    spec.md
```

## 变更 ID 命名

使用短横线小写命名：

```text
add-course-hide-flag
fix-course-access-for-founder
upgrade-springboot-3
split-order-service
```

## 后端架构边界

- Controller 只做参数接收、权限注解、响应包装，不写复杂业务逻辑。
- Service 承担业务编排和事务边界。
- Mapper/Repository 只做数据访问。
- Integration/Client 负责调用外部系统，例如 R2/S3、微信支付、ES、Kafka、远程服务。
- Config 只放配置装配，不放业务判断。
- DTO/VO/BO/Entity 分层清楚，接口对象不得直接泄露数据库内部结构。

## 当前技术基线

- Java 8 运行基线，CI 当前用 JDK 17 构建。
- Spring Boot 2.5.x。
- Spring Cloud 2020.x，Spring Cloud Alibaba 2021.x。
- MyBatis Plus、PageHelper、Druid、MySQL。
- Redis/Redisson。
- Elasticsearch。
- Nacos。
- Cloudflare R2/S3 兼容对象存储。
- Quartz/XXL-JOB。

## 升级原则

基础组件升级必须先建 OpenSpec change，并包含：

- 兼容性矩阵。
- 受影响模块。
- 配置迁移。
- 数据迁移或索引重建策略。
- 回滚方案。
- 灰度验证。
- 性能和稳定性风险。

## 自动化要求

所有 PR 都会触发 `Spec & Process Guard`：

- 检查基础规范文件是否存在。
- 检查 PR 是否包含 OpenSpec change，或在 PR 描述中声明 `OpenSpec-Exempt: true` 和豁免原因。
- 检查 change 包里是否有 `proposal.md`、`design.md`、`tasks.md`。

这不是形式主义。它的目标是让新人、老同事和 AI Agent 都围绕同一份事实工作。

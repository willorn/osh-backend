# OpenSpec Changes

每个中大型变更都在这里建立一个独立目录：

```text
openspec/changes/<change-id>/
  proposal.md
  design.md
  tasks.md
  specs/
    <capability>/
      spec.md
```

## 什么情况必须建 change

- 新功能。
- 修复会改变用户行为的 Bug。
- 改接口路径、参数、响应结构、错误码。
- 改权限、登录态、风控、支付、订单、课程权益。
- 改数据库表结构或数据含义。
- 改 Redis/Kafka/ES/Nacos/R2/S3 等中间件配置或使用方式。
- 升级 JDK、Spring Boot、Spring Cloud、Redis、Kafka、ES 等基础组件。
- 拆分微服务、调整模块边界、引入新依赖。

## 什么情况可以豁免

- 纯文案、注释、README 修正。
- 不影响行为的格式化。
- 明确无行为变化的小型样式或日志调整。
- 紧急生产修复，但修复后必须补齐 change。

豁免必须在 PR 描述中写：

```text
OpenSpec-Exempt: true
Reason: 只修正文档错别字，不影响运行行为。
```

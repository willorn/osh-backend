# 发布流程

## 分支流

```text
feature/codex 分支 -> PR -> qa/** -> release/** -> production
```

## QA 前

- PR 通过 Spec & Process Guard。
- 构建通过。
- Reviewer 通过。
- OpenSpec tasks 完成。

## Release 前

- 确认当前 release 分支是最新 release。
- 确认部署配置和目标服务器。
- 确认数据库、缓存、ES、对象存储变更。
- 准备回滚步骤。

## 生产发布后

- 检查健康接口。
- 检查关键业务日志。
- 检查核心接口 smoke。
- 记录发布结论。

## 热修

紧急热修可以先走快速路径，但必须：

- PR 描述写明紧急原因。
- 发布后补 OpenSpec change。
- 复盘缺失测试或监控。

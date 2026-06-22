# Proposal: course-announcement-deploy-verify

## 背景

课程公告接口已在 `course-announcement` 中实现。测试环境（juegeresource.top）经 HTTPS 访问时，前端需以同源 `/pc` 调用后端，构建流水线在打包阶段已将后端地址改写为 `/pc`。本次用于验证 course 模块经 feature → qa 自动部署到 25 测试机的流程是否顺畅，并将「课程公告接口经同源 /pc 可访问」固化为可验收要求。

## 目标

- 验证 course 模块改动经 `feature/course_hero_hope_xinghao` → `qa/20260708` 合并后自动部署到 25 测试机。
- 固化要求：课程公告查询接口经测试域名同源 `/pc` 可正常访问（HTTPS 下无 Mixed Content）。

## 非目标

- 不改动课程公告的业务逻辑与数据结构。
- 不改动部署流水线、nginx、higress 等基础设施配置。
- 不涉及生产（149）环境。

## 用户影响

- 用户经 `https://juegeresource.top` 访问课程列表页时，公告数据可正常加载。
- 现有课程公告接口行为不变。

## 风险

- 兼容性风险：低。仅新增注释与文档，无行为变更。
- 回滚风险：revert 本次 commit 即可。

## 验收标准

- [ ] course 模块改动经 qa 合并后触发 25 自动部署成功。
- [ ] `GET /pc/course/announcement/systemNotice/latest` 经同源 `/pc` 返回 200。
- [ ] `GET /pc/course/announcement/userNotice/latest` 经同源 `/pc` 返回 200。

## 关联信息

- 关联变更：`openspec/changes/course-announcement`
- 部署流水线：`.github/workflows/deploy-release.yml`（qa/** 自动部署到 25）

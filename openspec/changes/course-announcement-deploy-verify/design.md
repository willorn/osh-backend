# Design: course-announcement-deploy-verify

## 方案

本次为流程验证 + 可访问性要求固化，不引入新代码逻辑：

1. 在 `CourseAnnouncementController` 增加模块说明注释（无副作用）。
2. 经 `feature/course_hero_hope_xinghao` 提交，合并到 `qa/20260708` 触发 25 自动部署。
3. 部署后通过测试域名同源 `/pc` 验证课程公告接口可访问。

## 访问链路

```
浏览器 (https://juegeresource.top)
  → 前端 JS 同源调用 /pc/course/announcement/*
  → Cloudflare / higress / osh-nginx 反代
  → osh-backend:8081
```

前端构建时由部署流水线将 `http://43.242.200.25:8081` 改写为同源 `/pc`，避免 HTTPS 页面调 HTTP 接口被浏览器拦截（Mixed Content）。

## 备选方案

- 直接改源码写死 `/pc`：与现有「源码保持写死、构建时改写」的策略不一致，未采用。

## 影响面

- 仅 course 模块注释与 OpenSpec 文档，无运行时行为变化。

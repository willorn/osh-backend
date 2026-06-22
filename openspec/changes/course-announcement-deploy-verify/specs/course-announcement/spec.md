# Spec: Course Announcement Same-Origin Access

## ADDED Requirements

### Requirement: 课程公告接口经测试域名同源 /pc 可访问

系统 SHALL 保证课程公告查询接口在测试域名（HTTPS）下经同源 `/pc` 路径可访问，不出现 Mixed Content 拦截。

#### Scenario: HTTPS 域名查询系统通知

- GIVEN 用户经 `https://juegeresource.top` 打开课程列表页
- WHEN 前端以同源路径请求 `GET /pc/course/announcement/systemNotice/latest`
- THEN 请求经 nginx/higress 反代到 `osh-backend:8081`
- AND 响应 code=200

#### Scenario: HTTPS 域名查询业务动态

- GIVEN 用户经 `https://juegeresource.top` 打开课程列表页
- WHEN 前端以同源路径请求 `GET /pc/course/announcement/userNotice/latest`
- THEN 请求经 nginx/higress 反代到 `osh-backend:8081`
- AND 响应 code=200

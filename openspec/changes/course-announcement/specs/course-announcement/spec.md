# Spec: Course Announcement

## ADDED Requirements

### Requirement: 课程模块提供公告查询接口

系统 SHALL 提供两个匿名 GET 接口，分别返回课程模块最新 5 条系统通知与业务动态。

#### Scenario: 查询系统通知

- GIVEN `osh_announcement` 中存在 `resource_type='course'` 且 `channel=1` 的记录
- WHEN 客户端请求 `GET /pc/course/announcement/systemNotice/latest`
- THEN 响应 code=200，data 为按 create_time 降序的最多 5 条公告

#### Scenario: 查询业务动态

- GIVEN `osh_announcement` 中存在 `resource_type='course'` 且 `channel=2` 的记录
- WHEN 客户端请求 `GET /pc/course/announcement/userNotice/latest`
- THEN 响应 code=200，data 为按 create_time 降序的最多 5 条公告

### Requirement: 课程审核通过写入系统通知

系统 SHALL 在课程审核通过（published）时写入系统通知并广播 WebSocket 刷新消息。

#### Scenario: 课程审核通过

- GIVEN 课程资源审核结果为 published
- WHEN `CourseResourceAuditCallbackHandler` 处理回调
- THEN 写入 channel=1、resource_type=course 的公告
- AND 广播 `COURSE_USER_NOTICE_REFRESH` 消息

### Requirement: 课程购课成功写入业务动态

系统 SHALL 在用户课程订单 markPaid 成功后异步写入业务动态并通知用户。

#### Scenario: 购课支付成功

- GIVEN 课程订单首次 markPaid 成功
- WHEN `CoursePayServiceImpl` 完成支付状态更新
- THEN 异步写入 channel=2、resource_type=course 的购课公告
- AND 向购买者发送 `COURSE_PURCHASE_SUCCESS` 个人通知
- AND 广播 `COURSE_USER_NOTICE_REFRESH` 消息

### Requirement: 购课公告失败不阻断支付

系统 SHALL 在公告发布失败时不影响支付主流程。

#### Scenario: 公告 Publisher 异常

- GIVEN markPaid 已成功
- WHEN `CoursePurchaseAnnouncementPublisher` 抛出异常
- THEN 支付结果仍返回 paid=true
- AND 仅记录 warn 日志

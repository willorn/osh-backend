# Spec: Feedback Enhancement

## ADDED Requirements

### Requirement: 反馈状态变更邮件通知

系统 SHALL 在反馈状态发生变更时，异步向提交者发送邮件通知。

#### Scenario: 状态变更且用户有邮箱

- GIVEN 反馈提交者的 user 记录中 email 字段非空
- WHEN 反馈状态从 pending 变更为 processing/resolved/rejected
- THEN 系统异步发送包含反馈标题、新旧状态、跳转链接的邮件

#### Scenario: 状态变更但用户无邮箱

- GIVEN 反馈提交者的 user 记录中 email 为空
- WHEN 反馈状态发生变更
- THEN 系统跳过邮件发送，记录 info 日志

#### Scenario: 邮件发送失败

- GIVEN 邮件服务不可用
- WHEN 反馈状态变更触发邮件发送
- THEN 主流程不受影响，状态更新正常返回
- AND 系统记录 warn 日志

### Requirement: 公告 WebSocket 刷新广播

系统 SHALL 提供 `AnnouncementRefreshBroadcaster`，支持按 module 和 channel 广播 `ANNOUNCEMENT_REFRESH` 消息。

#### Scenario: 广播反馈模块公告刷新

- GIVEN 反馈模块有新的公告或动态产生
- WHEN 调用 `AnnouncementRefreshBroadcaster.broadcast("feedback", channel)`
- THEN 通过 WebSocket 向所有连接客户端发送 `ANNOUNCEMENT_REFRESH` 消息
- AND 消息体包含 module 和 channel 标识

### Requirement: 开源项目模块扩展

系统 SHALL 为开源项目提供技术组件库信息和公告查询能力。

#### Scenario: 查询开源项目详情

- GIVEN 开源项目关联了技术组件
- WHEN 客户端请求项目详情
- THEN 响应中包含技术组件库列表和公告数据

### Requirement: 首页反馈热门字段精简

系统 SHALL 精简首页反馈热门查询，移除冗余 VO 封装。

#### Scenario: 首页加载反馈热门

- GIVEN 首页需要展示反馈热门数据
- WHEN 执行反馈热门查询
- THEN 直接返回必要字段，不再经过 HotFeedbackVO 转换

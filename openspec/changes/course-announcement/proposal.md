# Proposal: course-announcement

## 背景

工具模块已接入统一公告栏（系统通知 + 业务动态），课程模块尚未对齐。产品需要在课程列表页展示课程相关公告，并在审核通过、购课成功等业务动作发生时自动写入公告并支持 WebSocket 刷新。

## 目标

- 课程模块写入 `osh_announcement`（`resource_type='course'`）。
- 提供课程公告查询接口（系统通知、业务动态）。
- 课程审核通过时写入系统通知并广播刷新消息。
- 课程购课成功时写入业务动态并广播刷新消息。
- 从默认审核回调中拆分课程专用处理器，避免重复通知。

## 非目标

- 本次不改造工具、首页等其他模块公告实现。
- 本次不新增数据库表或字段。
- 本次不改动共享支付控制器（`PayController` 等）。
- 本次不实现公告后台管理 CRUD。

## 用户影响

- 课程列表页可看到「系统通知」「课程动态」双栏公告。
- 课程审核通过后，系统通知栏展示上新信息。
- 用户购课成功后，课程动态栏展示购买信息。
- 现有课程搜索、详情、支付等接口行为不变。

## 风险

- 兼容性风险：低。新增只读接口，购课/审核为增量副作用，失败不阻断主流程。
- 数据风险：低。复用已有 `osh_announcement` 表，无 schema 变更。
- 性能风险：低。公告写入异步执行，查询 limit 5。
- 安全风险：低。查询接口 `@Anonymous`，与工具模块一致。
- 回滚风险：revert 本次 commit 即可，历史公告数据可保留。

## 验收标准

- [ ] `GET /pc/course/announcement/systemNotice/latest` 返回 200。
- [ ] `GET /pc/course/announcement/userNotice/latest` 返回 200。
- [ ] 课程审核通过后写入系统通知（channel=1）。
- [ ] 课程购课成功后写入业务动态（channel=2）并 WS 广播 `COURSE_USER_NOTICE_REFRESH`。
- [ ] `mvn compile` 通过，现有课程接口不受影响。

## 关联信息

- 参考实现：工具模块 `ToolAnnouncement*`、`ToolResourceAuditCallbackHandler`
- 文档：`docs/announcement/Announcement-Backend-Integration.md`
- PR：#258

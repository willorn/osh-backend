# Tasks: feedback-enhancement

## 准备

- [x] 确认邮件服务可用（smtp.qq.com）。
- [x] 参考课程模块公告与广播实现。

## 实现

- [x] 新增 `FeedbackStatusMailNotifier` 接口及 `FeedbackStatusMailNotifierImpl` 实现。
- [x] 新增 `AnnouncementRefreshBroadcaster` WebSocket 广播器。
- [x] `AssistantFeedbackServiceImpl` 接入邮件通知与广播。
- [x] 点赞/收藏/动态 Service 交互逻辑优化。
- [x] `OshAnnouncementMapper` 扩展查询方法。
- [x] `AnnouncementMarqueeVO` 字段调整 + 新增 `FeedbackDynamicVO`。
- [x] 开源项目模块 Controller/Service/Mapper 扩展。
- [x] 精简首页反馈热门字段，移除 `HotFeedbackVO`。
- [x] 新增 OpenSpec change 文档。

## 验证

- [x] 反馈状态变更后邮件发送成功。
- [x] WebSocket 收到 `ANNOUNCEMENT_REFRESH` 广播。
- [x] 开源项目扩展接口返回正确。
- [x] 首页反馈热门查询正常。
- [x] `mvn compile -Dmaven.test.skip=true` 通过。
- [x] `bash scripts/verify-ai-process.sh` 通过。

## PR

- [x] PR 描述关联 `openspec/changes/feedback-enhancement`。
- [x] 填写验证结果与回滚说明。

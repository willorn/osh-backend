# Tasks: course-announcement

## 准备

- [x] 阅读工具模块公告实现与 `Announcement-Backend-Integration.md`。
- [x] 确认 `osh_announcement` 表已存在，无需 SQL 迁移。

## 实现

- [x] 新增 `CourseAnnouncementController` 及查询 Service/Mapper。
- [x] 新增 `CourseResourceAuditCallbackHandler`，从 Default 移除 COURSE。
- [x] 新增 `CoursePurchaseAnnouncementPublisher`，接入 `CoursePayServiceImpl`。
- [x] 新增 OpenSpec change 文档。

## 验证

- [x] `GET /pc/course/announcement/systemNotice/latest` 返回 200。
- [x] `GET /pc/course/announcement/userNotice/latest` 返回 200。
- [x] 课程 search/detail/tags 接口正常。
- [x] `mvn compile -Dmaven.test.skip=true` 通过。
- [x] `bash scripts/verify-ai-process.sh` 通过。

## PR

- [x] PR 描述关联 `openspec/changes/course-announcement`。
- [x] 填写验证结果与回滚说明。

## 部署流水线验证

- [x] course 模块加无副作用注释，走 feature → qa 合并，验证 25 测试服务器自动部署流程。

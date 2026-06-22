# Tasks: course-announcement-deploy-verify

## 准备

- [x] 确认课程公告接口已实现（`course-announcement`）。
- [x] 确认 qa/** 推送会触发 25 自动部署。

## 实现

- [x] `CourseAnnouncementController` 增加模块说明注释（无副作用）。
- [x] 新增本 OpenSpec change 文档。

## 验证

- [ ] feature → qa 合并后，25 自动部署成功。
- [ ] `GET /pc/course/announcement/systemNotice/latest` 经同源 `/pc` 返回 200。
- [ ] `GET /pc/course/announcement/userNotice/latest` 经同源 `/pc` 返回 200。

## PR

- [x] PR 描述关联 `openspec/changes/course-announcement-deploy-verify`。
- [ ] 填写部署与验收结果。

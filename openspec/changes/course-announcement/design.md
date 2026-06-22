# Design: course-announcement

## 方案概览

对齐工具模块公告链路：业务动作写 `osh_announcement` → HTTP 查询 → WebSocket 通知前端刷新。所有新增代码限定在 `course` 包及最小必要的审核/支付接入点。

## 现状分析

- 相关模块：`controller/course`、`service/course`、`mapper/course`、`service/impl/audit`
- 已有表：`osh_announcement`（工具模块已使用，`resource_type='tool'`）
- 课程审核：`DefaultResourceAuditCallbackHandler` 含 `COURSE`，仅发通用 WS，不写公告
- 课程支付：`CoursePayServiceImpl.markPaidByOrderNoAndUserId` 成功后无公告副作用

## 详细设计

### 新增文件

| 路径 | 作用 |
|---|---|
| `CourseAnnouncementController` | 公告查询 HTTP 入口 |
| `ICourseAnnouncementService` / `CourseAnnouncementServiceImpl` | 按 channel 查询最新 5 条 |
| `OshCourseAnnouncementMapper` + XML | 读写 `osh_announcement`，`resource_type='course'` |
| `CourseAnnouncementVO` | 公告展示 VO |
| `CourseResourceAuditCallbackHandler` | 审核通过写系统通知 + WS |
| `CoursePurchaseAnnouncementPublisher` + Impl | 购课成功写业务动态 + WS |

### 修改文件

| 路径 | 改动 |
|---|---|
| `DefaultResourceAuditCallbackHandler` | 移除 `COURSE`，避免与专用处理器重复 |
| `CoursePayServiceImpl` | 支付 markPaid 成功后调用购课公告 Publisher |

### 接口设计

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/pc/course/announcement/systemNotice/latest` | 系统通知，channel=1 |
| GET | `/pc/course/announcement/userNotice/latest` | 业务动态，channel=2 |

响应：`R<List<CourseAnnouncementVO>>`，字段 `id/title/link/createTime`。

### 数据设计

写入 `osh_announcement`：

| 场景 | channel | resource_type | title 示例 | link 示例 |
|---|---|---|---|---|
| 审核通过 | 1 | course | 课程上新：「xxx」已上线 | `/course_detail/{id}` |
| 购课成功 | 2 | course | {username}购买了「{title}」 | `/course_detail/{id}` |

无需执行 SQL 迁移。

### WebSocket

- 审核通过 / 购课成功：`broadcast` 类型 `COURSE_USER_NOTICE_REFRESH`
- 购课个人通知：`send` 类型 `COURSE_PURCHASE_SUCCESS`

### 中间件影响

- Redis / Kafka / ES / Nacos：无

## 兼容性

- 不影响现有 `/pc/course/*` 接口契约。
- 公告写入失败仅打 warn 日志，不阻断支付与审核主流程。

## 测试计划

```bash
curl http://localhost:8081/pc/course/announcement/systemNotice/latest
curl http://localhost:8081/pc/course/announcement/userNotice/latest
curl -X POST http://localhost:8081/pc/course/search -H 'Content-Type: application/json' -d '{"pageNum":1,"pageSize":5}'
cd backstage-admin && mvn compile -Dmaven.test.skip=true
bash scripts/verify-ai-process.sh
```

## 发布与回滚

- 发布：合并 PR 后重启后端服务。
- 回滚：revert commit，重启服务；`osh_announcement` 中 course 记录可保留。

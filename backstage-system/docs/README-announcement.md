# 首页公告栏说明

## 当前实现

首页公告栏已经切换到当前方案：

- 未登录用户直接调用 HTTP 接口：
  - `GET /pc/homepage/announcement/notice`
  - `GET /pc/homepage/announcement/dynamic`
- 已登录用户建立 WebSocket 连接后，只接收“刷新事件”
- 前端收到刷新事件后，再主动调用 `/notice` 和 `/dynamic`
- 后端不再通过 WebSocket 推送完整公告数据
- 首页查询不再依赖 `module`、`color`、`is_top`、`event_code`

## 底表字段

首页公告使用当前 `osh_announcement` 字段：

| 字段 | 说明 |
|------|------|
| `id` | 公告 ID |
| `title` | 公告标题 |
| `link` | 跳转链接 |
| `icon_code` | 图标编码 |
| `channel` | 栏目：`1` 系统通知，`2` 业务动态 |
| `resource_type` | 资源类型 |
| `resource_id` | 资源 ID |
| `sort` | 排序值，越大越靠前 |
| `status` | 状态：`0` 草稿，`2` 待审核，`4` 已发布，`6` 已下架 |
| `start_time` | 生效开始时间 |
| `end_time` | 生效结束时间 |
| `source` | 来源 |
| `source_module` | 来源模块 |
| `delete_flag` | 逻辑删除标记 |
| `create_by/create_time` | 创建信息 |
| `update_by/update_time` | 更新信息 |

## 接口口径

### 1. 系统通知

```http
GET /pc/homepage/announcement/notice
```

说明：

- 查询 `channel = 1`
- 遍历首页资源类型枚举
- 每个 `resource_type` 取最近 `5` 条
- 返回扁平列表

响应示例：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 1,
      "title": "系统通知：课程专区已完成本周更新",
      "link": "/course/detail/101",
      "icon": "course_notice",
      "channel": 1,
      "resourceType": "course",
      "resourceId": 101,
      "startTime": "2026-06-20 10:00:00",
      "endTime": "2026-07-05 10:00:00",
      "createTime": "2026-06-20 10:00:00"
    }
  ]
}
```

### 2. 业务动态

```http
GET /pc/homepage/announcement/dynamic
```

说明：

- 查询 `channel = 2`
- 遍历首页资源类型枚举
- 每个 `resource_type` 取最近 `5` 条
- 返回扁平列表

## WebSocket 刷新事件

定时任务和登录后推送只发送刷新事件，不发送完整数据。

核心用途：

- 登录成功后给当前用户推一次刷新事件
- XXL-Job 每小时广播一次刷新事件
- 前端监听到事件后重新调用 `/notice`、`/dynamic`

## SQL 过滤与排序

首页实际查询条件：

```sql
SELECT
    id,
    title,
    link,
    icon_code AS icon,
    channel,
    resource_type AS resourceType,
    resource_id AS resourceId,
    start_time AS startTime,
    end_time AS endTime,
    create_time AS createTime
FROM osh_announcement
WHERE delete_flag = 0
  AND status IN (0, 2, 4)
  AND channel = #{channel}
  AND resource_type = #{resourceType}
  AND (start_time IS NULL OR start_time <= NOW())
  AND (end_time IS NULL OR end_time >= NOW())
ORDER BY sort DESC, IFNULL(update_time, create_time) DESC, create_time DESC, id DESC
LIMIT 5;
```

## 相关脚本

- [test-announcement-data.sql](/E:/juege/JayTatum/osh-backend/backstage-system/docs/test-announcement-data.sql)
- [test-announcement-data-by-resource.sql](/E:/juege/JayTatum/osh-backend/backstage-system/docs/test-announcement-data-by-resource.sql)
- [test_homepage_announcements.sql](/E:/juege/JayTatum/osh-backend/test_homepage_announcements.sql)

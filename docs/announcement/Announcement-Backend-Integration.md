# 公告栏后端对接说明

## 1. 文档目标

这份文档面向后端开发者和 AI 代码助手，说明如何在 `osh-backend` 的任意业务模块中接入统一公告栏能力。

目标包括两部分：

1. 让模块自己的公告数据能正确写入 `osh_announcement`
2. 让前端公告栏组件能通过 HTTP + WebSocket 正常展示并刷新

这份文档适用于：

- 工具模块
- 网站模块
- 信息差模块
- 课程模块
- 图书模块
- 考试模块
- 其他新增业务模块

## 2. 当前统一链路

当前项目的公告栏已经统一为下面这条链路：

1. 后端业务模块把公告数据写入 `osh_announcement`
2. 后端提供本模块“系统公告”和“业务动态”两个查询接口
3. 前端页面先通过 HTTP 拉取数据
4. 当业务数据有变化时，后端通过 WebSocket 推送“刷新消息”
5. 前端收到刷新消息后，重新请求两个 HTTP 接口

这里要特别注意：

- WebSocket 不负责直接传公告列表
- WebSocket 只负责告诉前端“你该重新拉取了”
- 真正显示的数据来自公告查询接口

### 2.1 用户支付成功时的完整联动示例

以“工具模块支付成功后刷新公告栏”为例，完整链路如下：

1. 用户在前端发起支付。
2. 后端支付回调或支付业务确认成功。
3. 后端在业务 Service 中写入一条新的公告记录到 `osh_announcement`：
   - `channel = 2`
   - `resource_type = tool`
   - `icon_code = payment`
   - `title = 某用户购买了图片打码工具`
   - `link = /tool/detail/5`
4. 后端写表成功后，构造一条公告刷新 WebSocket 消息：
   - `type = PAYMENT_STATUS_CHANGED`
   - `module = tool`
   - `action = paid`
   - `refresh = true`
   - `noticeApi = /pc/tool/announcement/notice`
   - `dynamicApi = /pc/tool/announcement/dynamic`
5. 后端通过 `webSocketNotifyService.send(...)` 或 `broadcast(...)` 推送给前端。
6. 前端 `useWebSocket.js` 收到消息后，生成刷新 key：

```text
PAYMENT_STATUS_CHANGED::tool::paid
```

7. 前端页面把这个 key 对应的时间戳作为 `refreshTrigger` 传给公告栏组件。
8. 公告栏组件重新请求：
   - `/tool/announcement/notice`
   - `/tool/announcement/dynamic`
9. 前端拿到最新数据并更新展示。

这里的核心原则是：

- 先写表
- 再发刷新消息

如果只发 WebSocket，不写入 `osh_announcement`，前端重新拉取后也看不到新数据。

## 3. 当前真实代码位置

### 3.1 公告查询公共服务

- `E:/juege/JayTatum/osh-backend/backstage-system/src/main/java/com/backstage/system/service/announcement/IAnnouncementMarqueeQueryService.java`
- `E:/juege/JayTatum/osh-backend/backstage-system/src/main/java/com/backstage/system/service/impl/announcement/AnnouncementMarqueeQueryServiceImpl.java`

### 3.2 公告表 Mapper

- `E:/juege/JayTatum/osh-backend/backstage-system/src/main/java/com/backstage/system/mapper/announcement/OshAnnouncementMapper.java`

### 3.3 公告刷新消息构造服务

- `E:/juege/JayTatum/osh-backend/backstage-system/src/main/java/com/backstage/system/service/announcement/IAnnouncementRefreshMessageService.java`
- `E:/juege/JayTatum/osh-backend/backstage-system/src/main/java/com/backstage/system/service/impl/announcement/AnnouncementRefreshMessageServiceImpl.java`

### 3.4 WebSocket 建连后统一触发器

- `E:/juege/JayTatum/osh-backend/backstage-system/src/main/java/com/backstage/system/aspect/announcement/AnnouncementWebSocketConnectHandler.java`
- `E:/juege/JayTatum/osh-backend/backstage-system/src/main/java/com/backstage/system/aspect/announcement/CommonAnnouncementWebSocketConnectAspect.java`

### 3.5 首页现有实现参考

- `E:/juege/JayTatum/osh-backend/backstage-system/src/main/java/com/backstage/system/service/impl/homepage/OshHomePageAnnouncementPushServiceImpl.java`
- `E:/juege/JayTatum/osh-backend/backstage-system/src/main/java/com/backstage/system/aspect/homepage/HomepageAnnouncementWebSocketConnectHandler.java`
- `E:/juege/JayTatum/osh-backend/backstage-system/src/main/java/com/backstage/system/controller/homepage/OshHomePageAnnouncementController.java`

## 4. 新模块接入要完成的四件事

如果一个新模块要接入公告栏，必须完成下面四件事：

1. 写入公告表
2. 提供查询接口
3. 业务变化时推送公告刷新 WS 消息
4. WebSocket 建连后给当前用户主动推送一次刷新消息

只有把这四条链路都打通，前端组件才能稳定工作。

## 5. `osh_announcement` 的定位

`osh_announcement` 是统一公告底表。

模块公告的展示数据不建议散落在各业务表里现查现拼，而是推荐在业务动作发生时，把要展示的内容同步写入 `osh_announcement`。

这样做的好处：

- 前端查询统一
- 各模块展示结构统一
- 排序、上下线、置顶、发布时间窗口都能统一管理
- 首页聚合展示也更容易

## 6. 表里哪些字段最关键

接公告栏时，最关键的是这些字段：

| 字段 | 说明 |
|---|---|
| `title` | 展示文案 |
| `link` | 点击跳转链接，推荐直接存前端真实路由 |
| `icon_code` | 图标编码，不再建议直接存 emoji |
| `channel` | `1=系统公告`，`2=业务动态` |
| `resource_type` | 模块标识，必须稳定，例如 `tool`、`website`、`info_gap` |
| `resource_id` | 关联业务资源 ID，可为空 |
| `sort` | 排序权重，越大越靠前 |
| `status` | 建议已发布使用 `4` |
| `source` | 来源，例如 `manual`、`system`、`module` |
| `source_module` | 来源模块，例如 `tool`、`website` |
| `start_time/end_time` | 生效窗口，可为空 |

推荐规则：

- `resource_type` 直接等于模块代码
- `link` 直接存前端真实路由
- `icon_code` 存业务语义编码，例如 `thumb_up`、`publish`、`payment`

## 7. 写入公告表怎么做

推荐方式是：

- 在模块自己的业务 Service 中写入
- 或抽出模块自己的 `AnnouncementService`
- 不建议在 Controller 里直接操作公告表

### 7.1 写入系统公告

系统公告通常表示：

- 规则更新
- 栏目上新
- 导航调整
- 资源上架
- 审核规则变化

系统公告固定写：

- `channel = 1`

### 7.2 写入业务动态

业务动态通常表示：

- 用户支付成功
- 用户点赞
- 新内容发布
- 审核通过
- 新工具上线

业务动态固定写：

- `channel = 2`

### 7.3 示例：工具模块写入两条公告

```java
announcementMapper.insertModuleAnnouncement(
    "工具系统通知：图片打码工具已更新",
    "/tool/detail/5",
    "publish",
    1,
    "tool",
    5L,
    100,
    "system",
    "tool",
    "system"
);

announcementMapper.insertModuleAnnouncement(
    "工具上新：「图片打码工具」已上线",
    "/tool/detail/5",
    "online",
    2,
    "tool",
    5L,
    95,
    "module",
    "tool",
    "system"
);
```

说明：

- 第一条是系统公告
- 第二条是业务动态
- 两条都写进统一底表

## 8. 后端必须提供两个查询接口

每个模块如果要接前端公告栏，建议都提供两条接口：

1. 系统公告接口
2. 业务动态接口

示例：

```java
@RestController
@RequestMapping("/pc/tool/announcement")
public class ToolAnnouncementController {

    @Autowired
    private IAnnouncementMarqueeQueryService announcementMarqueeQueryService;

    @GetMapping("/notice")
    public R<List<AnnouncementMarqueeVO>> notice() {
        return R.ok(announcementMarqueeQueryService.list(
                AnnouncementModuleEnum.TOOL,
                AnnouncementChannelEnum.SYSTEM_NOTICE,
                10
        ));
    }

    @GetMapping("/dynamic")
    public R<List<AnnouncementMarqueeVO>> dynamic() {
        return R.ok(announcementMarqueeQueryService.list(
                AnnouncementModuleEnum.TOOL,
                AnnouncementChannelEnum.USER_NOTICE,
                10
        ));
    }
}
```

说明：

- 这里推荐直接复用 `IAnnouncementMarqueeQueryService`
- 不要每个模块自己重复写一套 `SELECT`

## 9. 模块枚举要统一

如果新增业务模块，建议同步补全：

- `AnnouncementModuleEnum`

让查询和消息构造都能复用枚举，而不是 everywhere 写死字符串。

推荐枚举 code 直接与公告表 `resource_type` 保持一致。

例如：

```java
TOOL("tool", "工具模块")
```

## 10. WebSocket 刷新消息统一协议

当前前端已经只适配新协议。

后端发公告栏刷新消息时，`payload` 必须是：

```java
Map<String, Object> payload = new LinkedHashMap<>();
payload.put("module", module);
payload.put("action", action);
payload.put("refresh", refresh);
payload.put("noticeApi", noticeApi);
payload.put("dynamicApi", dynamicApi);
```

完整消息由：

- `type`
- `bizId`
- `content`

共同组成。

当前统一构造入口：

- `IAnnouncementRefreshMessageService#buildAnnouncementRefreshMessage(...)`

## 11. `buildAnnouncementRefreshMessage(...)` 的正确用法

当前方法签名已经支持通用化：

```java
WsNotifyMessage buildAnnouncementRefreshMessage(
        AnnouncementWsEventTypeEnum eventType,
        String module,
        String action,
        boolean refresh,
        String title,
        String noticeApi,
        String dynamicApi,
        boolean requireAuth
);
```

### 11.1 首页刷新示例

```java
announcementRefreshMessageService.buildAnnouncementRefreshMessage(
        AnnouncementWsEventTypeEnum.ANNOUNCEMENT_REFRESH,
        "homepage",
        "refresh",
        true,
        "首页公告刷新",
        "/pc/homepage/announcement/notice",
        "/pc/homepage/announcement/dynamic",
        true
);
```

### 11.2 工具模块刷新示例

```java
announcementRefreshMessageService.buildAnnouncementRefreshMessage(
        AnnouncementWsEventTypeEnum.ANNOUNCEMENT_REFRESH,
        "tool",
        "refresh",
        true,
        "工具公告刷新",
        "/pc/tool/announcement/notice",
        "/pc/tool/announcement/dynamic",
        true
);
```

### 11.3 支付成功触发工具公告刷新

```java
announcementRefreshMessageService.buildAnnouncementRefreshMessage(
        AnnouncementWsEventTypeEnum.PAYMENT_STATUS_CHANGED,
        "tool",
        "paid",
        true,
        "支付成功，刷新工具公告",
        "/pc/tool/announcement/notice",
        "/pc/tool/announcement/dynamic",
        true
);
```

这里的关键点是：

- 事件名可以不是 `refresh`
- 但只要要驱动公告栏刷新，就必须明确传 `refresh = true`

### 11.4 支付成功场景的标准消息

如果工具模块支付成功后要刷新工具公告栏，推荐直接这样构造：

```java
WsNotifyMessage message = announcementRefreshMessageService.buildAnnouncementRefreshMessage(
        AnnouncementWsEventTypeEnum.PAYMENT_STATUS_CHANGED,
        "tool",
        "paid",
        true,
        "支付成功，刷新工具公告",
        "/pc/tool/announcement/notice",
        "/pc/tool/announcement/dynamic",
        true
);
```

这条消息的含义是：

- `PAYMENT_STATUS_CHANGED`：这是一条支付状态变化消息
- `tool`：刷新工具模块公告栏
- `paid`：动作名是支付成功
- `refresh=true`：前端必须重新拉取公告接口

### 11.5 对应前端会怎么接

前端不会直接显示这条 WS 的正文。

前端只会把它当成刷新通知，并用下面这个 key 记录刷新标记：

```text
PAYMENT_STATUS_CHANGED::tool::paid
```

所以只要你后端改了：

- `type`
- `module`
- `action`

前端对应页面也要按同样的三元组读取 `refreshTrigger`。

## 12. 什么情况下 `refresh` 要传 `true`

统一规则：

只要这条 WebSocket 消息要驱动前端公告栏重新拉接口，就必须传：

```java
refresh = true
```

反过来：

如果这条消息只是普通业务通知，不是给公告栏用的，就不要传 `refresh=true`。

因为前端现在已经严格按这个字段判断。

## 13. 业务变化后如何推送刷新消息

推荐在这些时机发刷新消息：

- 公告写表成功后
- 审核状态变更后
- 支付成功后
- 资源上架后
- 定时任务同步完公告数据后

示例：

```java
WsNotifyMessage message = announcementRefreshMessageService.buildAnnouncementRefreshMessage(
        AnnouncementWsEventTypeEnum.ANNOUNCEMENT_REFRESH,
        "tool",
        "refresh",
        true,
        "工具公告刷新",
        "/pc/tool/announcement/notice",
        "/pc/tool/announcement/dynamic",
        true
);

webSocketNotifyService.broadcast(message);
```

如果只对单个用户刷新：

```java
webSocketNotifyService.send(userId, message);
```

### 13.1 支付成功后推荐的完整后端写法

以工具模块支付成功为例，推荐顺序如下：

```java
// 1. 先写入公告底表
announcementMapper.insertModuleAnnouncement(
        "用户购买了「图片打码工具」",
        "/tool/detail/5",
        "payment",
        2,
        "tool",
        5L,
        95,
        "module",
        "tool",
        "system"
);

// 2. 再推送刷新消息
WsNotifyMessage message = announcementRefreshMessageService.buildAnnouncementRefreshMessage(
        AnnouncementWsEventTypeEnum.PAYMENT_STATUS_CHANGED,
        "tool",
        "paid",
        true,
        "支付成功，刷新工具公告",
        "/pc/tool/announcement/notice",
        "/pc/tool/announcement/dynamic",
        true
);

webSocketNotifyService.broadcast(message);
```

如果只希望当前购买用户页面刷新，也可以改成：

```java
webSocketNotifyService.send(userId, message);
```

这取决于你的业务需求：

- 全站都应看到这条业务动态：用 `broadcast`
- 只有当前用户需要看到变化：用 `send`

## 14. WebSocket 建连后为什么还要主动推送一次

因为前端页面可能是：

- 先打开页面
- 后建立 WS
- 或用户刷新页面后刚刚重连成功

如果只依赖“后续业务变化时推送”，那么用户刚连上时可能拿不到最新公告状态。

所以当前项目已经统一成：

- WebSocket 建连成功后
- 通过注册器 + 模块处理器列表
- 对所有模块逐个触发一次“建连后处理”

核心代码在：

- `CommonAnnouncementWebSocketConnectAspect`
- `AnnouncementWebSocketConnectHandler`

## 15. 新模块如何接入建连后推送

你需要新建一个模块处理器，实现：

```java
public class ToolAnnouncementWebSocketConnectHandler implements AnnouncementWebSocketConnectHandler {

    @Autowired
    private IToolAnnouncementPushService toolAnnouncementPushService;

    @Override
    public void handle(Long userId, WebSocketSession session) {
        if (userId == null) {
            return;
        }
        toolAnnouncementPushService.pushAnnouncementsToUser(userId);
    }
}
```

关键点：

- 注册为 Spring Bean
- 实现 `AnnouncementWebSocketConnectHandler`
- 建议加 `@Order`

例如：

```java
@Order(110)
@Component
public class ToolAnnouncementWebSocketConnectHandler implements AnnouncementWebSocketConnectHandler
```

这样在一次 WebSocket 建连后，首页、工具、网站等模块都可以一起触发自己的公告刷新消息。

## 16. 首页模块为什么特殊

首页模块和普通模块不完全一样。

普通模块通常：

- 查询自己模块的公告
- 展示自己模块的公告

而首页模块通常是：

- 聚合多个模块的数据
- 通过模块资源类型汇总展示

同时首页目前是“读取统一底表聚合展示”，不是业务自己直接往 `module=homepage` 写公告。

所以首页更像“聚合读模型”，而不是一个独立业务公告写入源。

## 17. 如果不是首页模块，推荐怎么做

对普通模块，推荐标准方案是：

1. 业务发生变化
2. 把展示内容写入 `osh_announcement`
3. 提供 `/notice` 和 `/dynamic` 两个查询接口
4. 变化后发送刷新 WS
5. 建连时给当前用户主动补发一次刷新 WS

这样前端模块页面就能稳定接入公告栏组件。

## 18. 给 AI 的标准后端接入步骤

如果 AI 要为一个新模块接入公告栏，建议按下面顺序执行：

1. 确定模块 code，例如 `tool`
2. 在 `AnnouncementModuleEnum` 增加模块枚举
3. 设计本模块哪些场景写系统公告，哪些场景写业务动态
4. 在业务 Service 中写入 `osh_announcement`
5. 提供：
   - `/pc/{module}/announcement/notice`
   - `/pc/{module}/announcement/dynamic`
6. 新建模块自己的 PushService，封装：
   - `pushAll...`
   - `push...ToUser`
7. 通过 `buildAnnouncementRefreshMessage(...)` 构造统一 WS 消息
8. 业务变化时调用 `broadcast(...)` 或 `send(...)`
9. 新建 `AnnouncementWebSocketConnectHandler` 实现类
10. 建连时推送一次刷新消息

## 19. 常见错误

### 19.1 已经写入公告表，但前端看不到

优先检查：

- `status` 是否为 `4`
- `delete_flag` 是否为 `0`
- `resource_type` 是否与模块查询 code 一致
- `channel` 是否正确
- `link` 是否为空
- `start_time/end_time` 是否把当前时间过滤掉了

### 19.2 前端页面不刷新

优先检查 WS payload 是否满足：

```json
{
  "module": "tool",
  "action": "refresh",
  "refresh": true
}
```

如果 `refresh` 不是 `true`，前端公告栏不会刷新。

### 19.3 前端收到 WS 了，但跳转链接不对

说明你写入 `osh_announcement.link` 时存的不是当前前端真实路由。

推荐修法：

- 直接修正数据库写入规则

不推荐长期依赖前端路径重写做兜底。

## 20. 推荐最终规范

为了让后续任意模块都能被 AI 自动接入公告栏，推荐统一遵循下面规范：

1. 公告展示数据统一写入 `osh_announcement`
2. `resource_type` 直接等于模块 code
3. 每个模块统一提供：
   - `/notice`
   - `/dynamic`
4. WebSocket 刷新统一使用：
   - `type`
   - `module`
   - `action`
   - `refresh`
   - `noticeApi`
   - `dynamicApi`
5. 只要要刷新公告栏，必须显式传 `refresh=true`
6. WebSocket 建连后统一通过 `AnnouncementWebSocketConnectHandler` 列表触发模块刷新
7. `link` 统一存前端真实路由
8. `icon_code` 统一存图标语义编码

做到这八点后，AI 可以基于任意业务模块快速补齐公告栏能力，而不需要再改公共底层逻辑。

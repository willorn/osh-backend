# @DistributeLock 使用说明

## 1. 作用概述

`@DistributeLock` 是系统内统一的分布式锁注解，用于在多实例部署场景下控制方法并发执行。

当前实现特点：

- 基于 AOP 切面拦截
- 底层使用 Redisson `RLock`
- 支持固定 key
- 支持 SpEL 动态 key
- 支持自动拼接当前用户 ID
- 支持执行完成立即释放
- 支持“定时锁”模式，即方法执行结束也不主动释放，只等待超时自动释放

适用场景：

- 防重复提交
- 用户维度串行操作
- 同一资源的并发更新互斥
- 定时任务防重复执行
- 高风险接口限时防重试

## 2. 核心实现位置

注解定义：

- [backstage-common/src/main/java/com/backstage/common/annotation/DistributeLock.java](/Users/whiskey_liu/IdeaProjects/osh-backend/backstage-common/src/main/java/com/backstage/common/annotation/DistributeLock.java)

切面实现：

- [backstage-common/src/main/java/com/backstage/common/aspect/DistributeLockAspect.java](/Users/whiskey_liu/IdeaProjects/osh-backend/backstage-common/src/main/java/com/backstage/common/aspect/DistributeLockAspect.java)

## 3. 总体原理

### 3.1 基于 AOP 自动加锁

只要方法上加了：

```java
@DistributeLock(...)
```

切面 `DistributeLockAspect` 就会在方法执行前后自动处理锁。

整体流程：

```text
[请求进入 Controller / Service 方法]
        |
        v
[AOP 拦截到 @DistributeLock]
        |
        +--> 解析 scene
        +--> 解析 key 或 keyExpression
        +--> 判断是否拼接 userId
        +--> 组装最终 lockKey
        |
        v
[调用 RedissonClient.getLock(lockKey)]
        |
        +--> 加锁成功 -> 执行业务方法
        |
        +--> 加锁失败 -> 抛出 DistributeLockException
        |
        v
[业务方法执行完成]
        |
        +--> releaseImmediately=true  -> 立即 unlock
        |
        +--> releaseImmediately=false -> 不主动 unlock，等待 expireTime 到期
```

### 3.2 最终锁 key 结构

基础结构：

```text
scene#key
```

如果 `includeUserId = true` 且当前线程拿得到用户 ID，则会变成：

```text
scene#user:用户ID#key
```

例如：

```java
@DistributeLock(scene = "tool:save", key = "operation")
```

当前用户 ID 为 `10001`，最终锁 key：

```text
tool:save#user:10001#operation
```

## 4. 注解参数说明

注解定义：

```java
public @interface DistributeLock {

    String scene() default "";

    String key() default "NONE";

    String keyExpression() default "NONE";

    boolean includeUserId() default true;

    int expireTime() default -1;

    int waitTime() default -1;

    boolean releaseImmediately() default true;
}
```

参数说明如下。

### 4.1 scene

业务场景名，用于区分锁域。

推荐：

```java
scene = "tool:update"
scene = "course:audit"
scene = "order:create"
```

不推荐用空字符串，否则 Redis 中的 key 不够直观。

### 4.2 key

固定锁 key。

示例：

```java
@DistributeLock(scene = "resource", key = "operation")
```

适合：

- 某类固定操作防重复
- 用户维度固定行为串行化

### 4.3 keyExpression

支持 SpEL 表达式，从方法参数中动态提取 key。

示例：

```java
@DistributeLock(scene = "tool:update", keyExpression = "#request.id", includeUserId = false)
```

当 `request.id = 2001` 时，锁 key 为：

```text
tool:update#2001
```

适合：

- 按资源 ID 加锁
- 同一资源互斥，不同资源并发

### 4.4 includeUserId

是否自动拼接当前用户 ID。

默认值：

```java
includeUserId = true
```

效果：

```text
scene#user:10001#key
```

适合：

- 防止同一个用户重复提交
- 不同用户之间允许并发

### 4.5 expireTime

锁超时时间，单位毫秒。

说明：

- `-1` 表示使用 Redisson 默认续期机制
- 显式设置后，锁会在指定时间后自动失效

例如：

```java
expireTime = 60000
```

表示锁 60 秒后自动过期。

### 4.6 waitTime

获取锁等待时长，单位毫秒。

说明：

- `-1` 表示阻塞等待
- `0` 表示立即尝试，失败就返回
- 大于 `0` 表示等待指定时间

例如：

```java
waitTime = 3000
```

表示最多等 3 秒获取锁。

### 4.7 releaseImmediately

是否在方法执行结束后立即释放锁。

默认值：

```java
releaseImmediately = true
```

分两种模式。

## 5. 两种锁模式

### 5.1 普通锁：执行完立即释放

配置：

```java
releaseImmediately = true
```

行为：

- 业务执行完成后，在 `finally` 里立即 `unlock`
- 适合大多数提交、更新、审核类接口

流程：

```text
[拿到锁]
   |
   v
[执行业务]
   |
   v
[finally 中立即 unlock]
```

示例：

```java
@PostMapping("/save")
@DistributeLock(
        scene = "tool:save",
        key = "operation",
        expireTime = 10000,
        waitTime = 3000,
        releaseImmediately = true
)
public R<Long> save(@RequestBody ToolSaveRequest request) {
    ...
}
```

适合：

- 新增工具
- 提交课程
- 下单防重复
- 资源审核

### 5.2 定时锁：执行完不释放，只等超时

配置：

```java
releaseImmediately = false
```

并且必须显式配置：

```java
expireTime
```

否则切面会直接抛异常：

```text
expireTime must be configured when releaseImmediately is false
```

行为：

- 方法执行结束后不调用 `unlock`
- 锁一直保留到 `expireTime` 到期
- 即使接口很快执行完，锁也不会提前释放

这就是你说的“锁 1 分钟，就算接口调用完毕了也不释放，只有 1 分钟后才能重新拿锁”的模式。

流程：

```text
[拿到锁]
   |
   v
[执行业务方法]
   |
   v
[方法结束]
   |
   +--> 不 unlock
   |
   v
[等待 expireTime 到期]
   |
   v
[锁自动失效]
```

### 5.3 1 分钟定时锁示例

```java
@PostMapping("/retry-protected")
@DistributeLock(
        scene = "tool:highRisk",
        keyExpression = "#request.toolId",
        includeUserId = true,
        waitTime = 0,
        expireTime = 60000,
        releaseImmediately = false
)
public R<String> highRiskAction(@RequestBody ToolUsageConsumeRequest request) {
    return R.ok("提交成功，1分钟内不可重复触发");
}
```

行为说明：

- 用户 `10001` 对 `toolId=88` 发起请求
- 锁 key：

```text
tool:highRisk#user:10001#88
```

- 接口执行 200ms 就结束
- 但锁不会释放
- 60 秒内同一用户再次请求同一个工具会直接拿锁失败
- 60 秒后才能再次成功

适合：

- 高风险操作防重试
- 异步提交后的短时间冷却
- 支付确认/发放额度等需要短时间防抖的接口

## 6. 通过用户 ID 加锁

### 6.1 原理

当：

```java
includeUserId = true
```

切面会从线程上下文中读取当前用户 ID：

- `ThreadLocalUtil`
- `OshUserConstants.USER_ID`

然后把用户 ID 拼进锁 key。

### 6.2 效果

例如：

```java
@DistributeLock(scene = "tool:purchase", key = "submit", includeUserId = true)
```

两个用户同时请求：

```text
用户A -> tool:purchase#user:10001#submit
用户B -> tool:purchase#user:10002#submit
```

他们互不影响。

但同一个用户重复提交：

```text
用户A 第1次 -> tool:purchase#user:10001#submit
用户A 第2次 -> tool:purchase#user:10001#submit
```

就会竞争同一把锁。

### 6.3 适用场景

推荐用于：

- 用户提交订单
- 用户购买工具
- 用户发起支付
- 用户收藏/取消收藏防连点
- 用户个人配置更新

## 7. key 与 keyExpression 的使用建议

### 7.1 用 `key`

适合固定业务动作：

```java
@DistributeLock(scene = "tool:save", key = "operation")
```

适用于：

- 新增提交
- 发布操作
- 防止用户连续点击同一个按钮

### 7.2 用 `keyExpression`

适合资源级互斥：

```java
@DistributeLock(scene = "tool:update", keyExpression = "#request.id", includeUserId = false)
```

适用于：

- 修改工具
- 审核课程
- 删除某个资源
- 按资源 ID 串行处理

## 8. 典型流程图

### 8.1 用户维度防重复提交

```text
[用户点击“保存工具”]
        |
        v
[进入 Controller.save]
        |
        v
[AOP 生成锁 key: tool:save#user:10001#operation]
        |
        +--> 获取成功 -> 执行业务
        |
        +--> 获取失败 -> 返回“重复提交/请稍后重试”
        |
        v
[业务执行完成]
        |
        v
[立即释放锁]
```

### 8.2 资源维度互斥更新

```text
[管理员A修改工具1001]
[管理员B修改工具1001]
        |
        v
[AOP 统一生成锁 key: tool:update#1001]
        |
        +--> A 先拿到锁并执行
        |
        +--> B 等待或直接失败
```

### 8.3 定时锁防 1 分钟内重复提交

```text
[用户提交高风险操作]
        |
        v
[AOP 加锁成功]
        |
        v
[业务方法很快执行完成]
        |
        v
[不主动释放锁]
        |
        v
[1分钟内再次请求 -> 拿锁失败]
        |
        v
[1分钟到期 -> 锁自动失效]
        |
        v
[用户可再次请求]
```

## 9. 代码示例

### 9.1 普通接口防重复提交

```java
@PostMapping("/save")
@DistributeLock(
        scene = "tool:save",
        key = "operation",
        includeUserId = true,
        waitTime = 3000,
        expireTime = 10000,
        releaseImmediately = true
)
public R<Long> save(@Validated @RequestBody ToolSaveRequest request) {
    return R.ok(oshToolService.createTool(request, UserContextUtil.getCurrentUser()));
}
```

说明：

- 同一个用户 3 秒内重复点保存会等待锁
- 最多等待 3 秒
- 锁最长 10 秒
- 方法结束立刻释放

### 9.2 按资源 ID 更新

```java
@PostMapping("/update")
@DistributeLock(
        scene = "tool:update",
        keyExpression = "#request.id",
        includeUserId = false,
        waitTime = 0,
        expireTime = 10000,
        releaseImmediately = true
)
public R<Long> update(@Validated @RequestBody ToolSaveRequest request) {
    return R.ok(oshToolService.updateTool(request, UserContextUtil.getCurrentUser()));
}
```

说明：

- 同一个工具 ID 不能并发更新
- 不同工具 ID 可以并发
- 拿不到锁立即失败

### 9.3 用户维度 1 分钟定时锁

```java
@PostMapping("/purchase/submit")
@DistributeLock(
        scene = "tool:purchase",
        keyExpression = "#request.toolId",
        includeUserId = true,
        waitTime = 0,
        expireTime = 60000,
        releaseImmediately = false
)
public R<String> submitPurchase(@RequestBody ToolPurchaseCreateRequest request) {
    return R.ok("下单请求已受理，1分钟内不可重复提交");
}
```

说明：

- 同一用户对同一工具 1 分钟内只能提交一次
- 接口即使 100ms 内返回，也不会释放锁
- 到 60 秒后才允许再次提交

### 9.4 定时任务防重复执行

```java
@Scheduled(cron = "0 */5 * * * ?")
@DistributeLock(
        scene = "job:toolSync",
        key = "global",
        includeUserId = false,
        waitTime = 0,
        expireTime = 300000,
        releaseImmediately = true
)
public void syncTools() {
    ...
}
```

说明：

- 多实例部署下，同一时间只有一个实例执行同步任务

## 10. 使用建议

推荐：

- 用户防重复提交：`includeUserId=true`
- 资源级串行更新：`keyExpression + includeUserId=false`
- 高风险防短时重试：`releaseImmediately=false + expireTime`

不推荐：

- 不设置 `scene`
- `releaseImmediately=false` 但不配 `expireTime`
- 锁粒度过大，导致不同资源也串行
- 锁粒度过小，导致该互斥的操作没有真正互斥

## 11. 常见误区

### 11.1 接口执行完就一定释放锁

不一定。

如果：

```java
releaseImmediately = false
```

则方法结束后不会主动释放锁，只能等过期。

### 11.2 includeUserId=true 就是全局锁

不是。

这是用户维度锁，不同用户会得到不同锁 key。

### 11.3 keyExpression 适合所有场景

不是。

如果只是单纯防用户重复点击提交，用固定 `key` 往往更简单。

### 11.4 waitTime=0 会自动重试

不会。

`waitTime=0` 的含义是立即尝试，失败就直接返回。

## 12. 一句话总结

`@DistributeLock` 的本质是：

- 通过 AOP 在方法执行前后自动加 Redis 锁
- 可按用户维度、资源维度或全局维度控制并发
- 可选择执行完立即释放，也可选择“定时锁”模式延迟释放

如果你的诉求是“用户调完接口后，1 分钟内不允许再次触发”，正确配置就是：

```java
@DistributeLock(
        scene = "...",
        keyExpression = "...",
        includeUserId = true,
        waitTime = 0,
        expireTime = 60000,
        releaseImmediately = false
)
```

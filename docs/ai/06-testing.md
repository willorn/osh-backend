# 后端测试规范

## 最低验证

每个 PR 至少说明运行过什么验证。如果没有运行，必须写原因。

推荐基础命令：

```bash
mvn clean install -DskipTests -pl '!backstage-flink'
```

模块测试：

```bash
mvn test -pl backstage-system
```

## 接口 smoke

接口变更必须给出 smoke 范围：

- 登录态接口。
- 公开接口。
- 管理后台接口。
- 权限失败场景。
- 参数错误场景。

## 中间件验证

涉及 Redis/Kafka/ES/Nacos/R2/S3 时，必须验证：

- 连接配置。
- 失败降级。
- 超时行为。
- 重试和幂等。
- 生产/QA 差异。

## 测试数据

- 不使用生产真实隐私数据。
- 测试账号、测试订单、测试课程要可复用。
- 涉及支付必须用沙箱或明确的测试通道。

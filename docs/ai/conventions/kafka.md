# Kafka 约定

当前项目会逐步引入或加强 Kafka 能力。新增 Kafka 使用必须先写 OpenSpec。

## Topic

- Topic 按业务域命名。
- 事件名使用过去式，例如 `order-paid`、`course-access-granted`。

## 消息

- 必须包含事件 ID。
- 必须包含发生时间。
- 必须包含业务主键。
- 消费端必须幂等。

## 失败处理

- 明确重试次数。
- 明确死信或补偿方式。
- 明确监控告警。

# 后端架构说明

## 当前形态

当前后端是 Spring Boot 多模块单体：

- `backstage-admin`：启动入口、后台 Controller、应用装配。
- `backstage-common`：公共工具、基础类型、通用异常、工具类。
- `backstage-framework`：Web 框架、安全、Redis、拦截器、配置。
- `backstage-system`：主要业务域。
- `backstage-quartz`：定时任务。
- `backstage-generator`：代码生成。
- `backstage-flink`：Flink 相关计算。
- `backstage-hbase`：HBase 相关能力。

## 分层规则

```text
Controller -> Service -> Mapper/Repository -> Database
                      -> Integration Client -> External System
```

Controller 不写复杂业务。Service 是业务主场。Mapper 不做业务判断。外部系统调用必须隔离在 client/integration 层。

## 未来微服务方向

优先按业务能力拆：

- user-service：用户、登录、权限、邀请。
- course-service：课程、章节、学习权益。
- order-service：订单、支付、优惠券。
- tool-service：工具、使用次数、积分/现金购买。
- content-service：网站、资源、电子书、搜索。
- feedback-service：反馈、公告、处理记录。

拆分前必须先有 OpenSpec change，不能从“移动代码文件”开始。

# 工具模块 Flink 设计说明

## 1. 目标与整体职责

工具模块的 Flink Job 负责把 Kafka 中的工具索引事件消费出来，并同步到 Elasticsearch 的工具搜索索引中。

当前工具索引同步任务主入口是：

- [ToolSearchIndexSyncJob.java](/Users/whiskey_liu/IdeaProjects/osh-backend/backstage-flink/src/main/java/com/bachstage/tool/job_stream_from_kafka_to_es/ToolSearchIndexSyncJob.java)

它的职责可以概括成三件事：

1. 从 Kafka 消费工具索引统一 Topic 的消息
2. 根据 `eventType` 将消息路由到不同的 ES 写入策略
3. 通过 Flink Checkpoint / Restart 保证任务在异常情况下可恢复

当前默认配置在：

- [application.properties](/Users/whiskey_liu/IdeaProjects/osh-backend/backstage-flink/src/main/resources/application.properties)

其中工具模块关键配置是：

```properties
tool.index.group-id=backstage-tool-index-flink
tool.index.start-mode=earliest
tool.index.topic=osh.tool.index
tool.index.es-index=osh_tool_search
```

## 2. 工具模块 Job 的核心流程

### 2.1 启动入口

Job 启动时会先加载配置：

- [ToolIndexJobConfig.java](/Users/whiskey_liu/IdeaProjects/osh-backend/backstage-flink/src/main/java/com/bachstage/tool/job_stream_from_kafka_to_es/config/ToolIndexJobConfig.java)

这层配置读取顺序是：

1. JVM System Property
2. 环境变量
3. `application.properties` 默认值

也就是说，容器里通过环境变量覆盖最方便，比如：

- `KAFKA_BOOTSTRAP_SERVERS`
- `TOOL_INDEX_GROUP_ID`
- `TOOL_INDEX_TOPIC`
- `TOOL_ES_INDEX`

### 2.2 Flink 运行参数

在 [ToolSearchIndexSyncJob.java](/Users/whiskey_liu/IdeaProjects/osh-backend/backstage-flink/src/main/java/com/bachstage/tool/job_stream_from_kafka_to_es/ToolSearchIndexSyncJob.java) 中，任务会统一设置：

1. `parallelism`
2. Checkpoint
3. RestartStrategy
4. Kafka consumer properties

关键点：

- `CheckpointingMode.EXACTLY_ONCE`
- `RETAIN_ON_CANCELLATION`
- 固定延迟重启

这意味着：

- 正常取消任务后，Checkpoint 不会被自动删掉
- 任务失败后会按配置自动重试

### 2.3 Kafka 消息进入 Flink 后的处理

工具模块 Kafka 消息是统一 Topic：

- `osh.tool.index`

在 `buildToolEventStream(...)` 中，消息处理顺序是：

1. 读原始字符串
2. JSON 解析
3. 过滤掉非 JSON 数据
4. 过滤掉没有 `id` 或没有 `eventType` 的非法消息

因此这个 Job 的输入协议非常清晰：

- 必须是 JSON
- 必须有 `id`
- 必须有 `eventType`

## 3. eventType 如何路由

在 [ToolSearchIndexSyncJob.java](/Users/whiskey_liu/IdeaProjects/osh-backend/backstage-flink/src/main/java/com/bachstage/tool/job_stream_from_kafka_to_es/ToolSearchIndexSyncJob.java) 中，当前工具模块把消息分成三类：

### 3.1 常规 Upsert

事件类型：

- `TOOL_INDEX_CREATE`
- `TOOL_INDEX_UPDATE`
- `TOOL_INDEX_COUNTER`

这些事件会进入：

- `buildUpsertSink(config)`

对应文件：

- [ToolIndexElasticsearchSinkFactory.java](/Users/whiskey_liu/IdeaProjects/osh-backend/backstage-flink/src/main/java/com/bachstage/tool/job_stream_from_kafka_to_es/es/ToolIndexElasticsearchSinkFactory.java)

### 3.2 审核状态部分更新

事件类型：

- `AUDIT_APPROVED`
- `AUDIT_REJECTED`

这些事件会进入：

- `buildAuditPartialUpdateSink(config)`

它只更新 ES 文档中的：

- `status`
- `updateBy`
- `updateTime`

不会覆盖整份工具文档。

### 3.3 删除事件

事件类型：

- `TOOL_INDEX_DELETE`

这些事件会进入：

- `buildDeleteSink(config)`

最终在 ES 中删除对应文档。

## 4. ES 全量更新与部分更新

这是这个工具模块设计里最重要的点之一。

### 4.1 整个更新 ES：Upsert

在：

- [ToolIndexElasticsearchSinkFactory.java](/Users/whiskey_liu/IdeaProjects/osh-backend/backstage-flink/src/main/java/com/bachstage/tool/job_stream_from_kafka_to_es/es/ToolIndexElasticsearchSinkFactory.java)

的 `buildUpsertSink(...)` 中，Flink 最终构造的是：

- `IndexRequest`

也就是按同一个文档 ID 重新写入整份文档。

这里的设计含义是：

- 新建工具：写整份文档
- 修改工具：写整份文档
- 计数更新：也写整份文档

这种方式的优点：

1. 结构简单
2. 不容易遗漏字段
3. ES 侧最终文档状态和后端构造的索引消息保持一致

代价是：

- 一次小字段变更也要写整份文档

### 4.2 部分更新 ES：Partial Update

审核事件没有走整份 upsert，而是走：

- `UpdateRequest`

这部分在：

- `AuditPartialUpdateSinkFunction`

中实现。

只更新最小必要字段：

- `status`
- `updateBy`
- `updateTime`

这么设计的原因是：

1. 审核只关心状态变化
2. 不希望审核消息把其他字段覆盖掉
3. 审核事件消息体本身也只携带最小字段

这和后端对应得上：

- [AuditIndexMessage.java](/Users/whiskey_liu/IdeaProjects/osh-backend/backstage-system/src/main/java/com/backstage/system/service/audit/AuditIndexMessage.java)

它本来就是为 ES partial update 设计的最小消息模型。

### 4.3 删除 ES

删除不做更新，而是直接：

- `DeleteRequest`

这能保证下架/删除后的工具文档及时从索引中清掉。

## 5. 时间格式：ES、MySQL、JVM 三者怎么对齐

这部分非常关键，不然最容易出现：

- Flink 能消费
- ES 能写入
- 但时间字段映射报错

### 5.1 后端 Java / MySQL 的默认时间格式

你们仓库当前后端约定是：

- Java 时间类型优先使用 `LocalDateTime`
- 默认格式是 `yyyy-MM-dd HH:mm:ss`

例如：

- [OshTool.java](/Users/whiskey_liu/IdeaProjects/osh-backend/backstage-system/src/main/java/com/backstage/system/domain/tool/OshTool.java)
- [FlexibleLocalDateTimeDeserializer.java](/Users/whiskey_liu/IdeaProjects/osh-backend/backstage-system/src/main/java/com/backstage/system/jackson/FlexibleLocalDateTimeDeserializer.java)

支付模块里也有统一格式化器：

- [OrderServiceImpl.java](/Users/whiskey_liu/IdeaProjects/osh-backend/backstage-system/src/main/java/com/backstage/system/service/order/impl/OrderServiceImpl.java)

说明当前后端对外、对 Kafka、对 JSON 的字符串时间主格式就是：

```text
yyyy-MM-dd HH:mm:ss
```

同时兼容毫秒：

```text
yyyy-MM-dd HH:mm:ss.SSS
```

### 5.2 Flink 内部如何处理时间

工具模块 Flink 没有直接把字符串时间原样写给 ES。

在：

- [ToolIndexJsonSerializer.java](/Users/whiskey_liu/IdeaProjects/osh-backend/backstage-flink/src/main/java/com/bachstage/tool/job_stream_from_kafka_to_es/serialize/ToolIndexJsonSerializer.java)

中，`createTime` 和 `updateTime` 会被统一转换成：

- `epoch_millis`

也就是毫秒时间戳。

审核 partial update 里也是一样：

- [ToolIndexElasticsearchSinkFactory.java](/Users/whiskey_liu/IdeaProjects/osh-backend/backstage-flink/src/main/java/com/bachstage/tool/job_stream_from_kafka_to_es/es/ToolIndexElasticsearchSinkFactory.java)

通过 `toEpochMillis(...)` 转成毫秒时间戳。

### 5.3 JVM 默认时区为什么会影响时间

`LocalDateTime -> epochMillis` 这个转换依赖：

```java
ZoneId.systemDefault()
```

也就是说，**Flink 容器的 JVM 默认时区会影响写入 ES 的最终时间戳**。

如果：

- 后端生产消息时按东八区理解字符串时间
- Flink 容器系统时区却不是东八区

那么最终写入 ES 的 epoch 毫秒就会偏移。

### 5.4 当前服务器 docker-compose 的时区现状

根据服务器上的：

- `/opt/docker-apps/docker-compose.yml`

当前 Flink 容器环境变量里没有显式写：

- `TZ=Asia/Shanghai`

而 `backstage-admin`、`mysql8` 等服务是显式按上海时区在运行的。

这意味着：

- Flink 容器时区最好也显式统一成 `Asia/Shanghai`

建议在 `docker-compose.yml` 的 Flink 服务中补：

```yaml
environment:
  - TZ=Asia/Shanghai
  - |
    FLINK_PROPERTIES=
    ...
```

### 5.5 ES mapping 为什么建议同时兼容字符串和毫秒

你们仓库规范里已经明确提过：

```json
"format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd HH:mm:ss.SSS||strict_date_optional_time||epoch_millis"
```

这是非常合理的，因为：

1. 后端可能直接传字符串
2. Flink 这里会传 `epoch_millis`
3. 以后某些同步链路可能又直接传 ISO8601

所以工具模块 ES 索引中的时间字段，建议始终使用这个兼容格式。

## 6. 工具模块 Flink Job 和后端索引消息的关系

工具模块后端写索引消息的关键链路在：

- [OshToolServiceImpl.java](/Users/whiskey_liu/IdeaProjects/osh-backend/backstage-system/src/main/java/com/backstage/system/service/impl/tool/OshToolServiceImpl.java)
- [OutboxEventServiceImpl.java](/Users/whiskey_liu/IdeaProjects/osh-backend/backstage-system/src/main/java/com/backstage/system/service/impl/outbox/OutboxEventServiceImpl.java)

后端并不是直接同步 ES，而是：

1. 先构造 `ToolIndexMessage`
2. 写入 Outbox
3. Outbox 投递到 Kafka
4. Flink 消费 Kafka
5. Flink 写入 ES

这么做的好处：

1. 业务事务和索引更新解耦
2. ES 短暂故障不会直接阻塞主业务
3. Flink 可以统一处理写入、重试、路由

## 7. 服务器上的 Flink 容器结构

当前服务器真正生效的 Flink 部署不是旧的 `/opt/flink/docker-compose.yml`，而是：

- `/opt/docker-apps/docker-compose.yml`

这一点非常重要。

当前服务器上实际运行的是：

- `flink-jobmanager-1`
- `flink-taskmanager-1`
- `flink-taskmanager-2`

镜像版本：

- `flink:1.18.1-scala_2.12-java8`

### 7.1 Jar 挂载位置

在 compose 中，Flink 容器的 usrlib 是这样挂载的：

```yaml
volumes:
  - /home/flink/jars:/opt/flink/usrlib
```

这意味着：

- **服务器宿主机目录**：`/home/flink/jars`
- **容器内目录**：`/opt/flink/usrlib`

所以你本地重新打包后的 jar，真正要替换的是：

```text
/home/flink/jars/backstage-flink-3.9.0.jar
```

而不是别的地方。

## 8. 为什么要设计一个“提交 Job 的服务/脚本”

当前 compose 只负责：

1. 起 JobManager
2. 起 TaskManager
3. 挂载 Jar

但 **compose 本身不会自动帮你重新提交 Flink Job**。

容器起来后，真正的 Job 提交动作通常还是：

```bash
docker exec flink-jobmanager-1 flink run -d -c <MainClass> /opt/flink/usrlib/backstage-flink-3.9.0.jar
```

因此如果每次发版都手工：

1. 替换 jar
2. cancel old job
3. restart container
4. 手工 run 两三个 main class

会有几个问题：

1. 易忘
2. 易提错主类
3. 容易只提一个 Job，漏掉另一个 Job
4. 不利于后续统一交接

所以更合理的做法是：

- 单独设计一个“提交 Flink Job 的脚本”或“submit-job 服务”

它的职责是把这几个动作标准化。

## 9. 当前服务器上的一键提交流程

这一节不再讲抽象示例，而是直接按服务器现网配置说明。

当前服务器已经不是“手工 `docker exec flink run`”的模式，而是专门设计了一个：

- `flink-submitter`

来负责提交 Flink Job。

### 9.1 现网 `flink-submitter` 容器是怎么设计的

在服务器的：

- `/opt/docker-apps/docker-compose.yml`

里，当前有这样一个服务：

```yaml
flink-submitter:
  logging: *default-logging
  image: flink:1.18.1-scala_2.12-java8
  container_name: flink-submitter
  restart: "no"
  depends_on:
    - flink-jobmanager-1
  command: ["/bin/sh", "/opt/flink/scripts/flink-submit-jobs.sh"]
  environment:
    - |
      FLINK_PROPERTIES=
      # flink-submitter 通过 Docker 内网把作业提交到 JobManager。
      jobmanager.rpc.address: flink-jobmanager-1
      # 这里的 RPC 端口用于和 JobManager 建立集群内部通信。
      jobmanager.rpc.port: 30082
      # 这里的 REST 端口用于提交、查询和管理作业。
      rest.port: 30081
      state.checkpoints.dir: file:///opt/flink/checkpoints
      state.savepoints.dir: file:///opt/flink/savepoints
      execution.checkpointing.interval: 60000
      execution.checkpointing.externalized-checkpoint-retention: RETAIN_ON_CANCELLATION
    - KAFKA_BOOTSTRAP_SERVERS=osh-kafka:9092
  volumes:
    - /home/flink/checkpoints:/opt/flink/checkpoints
    - /home/flink/savepoints:/opt/flink/savepoints
    - /home/flink/jars:/opt/flink/usrlib
    - /opt/docker-apps/flink/flink-submit-jobs.sh:/opt/flink/scripts/flink-submit-jobs.sh:ro
  networks:
    - app-network
```

这个设计的核心思想是：

1. `flink-jobmanager-1` 负责提供 Flink 集群控制面
2. `flink-taskmanager-*` 负责执行任务
3. `flink-submitter` 不常驻跑业务，它只是一个“提交器”
4. 容器启动后，自动执行：

```bash
/opt/flink/scripts/flink-submit-jobs.sh
```

也就是说，**真正负责“把 Job 提交到 Flink 集群”的不是 JobManager，而是 submitter 容器**。

### 9.2 为什么要单独设计一个 `flink-submitter`

这个设计比手工提交更合理，原因有四个：

#### 9.2.1 职责分离

- JobManager：负责集群管理
- TaskManager：负责执行任务
- Submitter：负责作业提交

这样每个容器职责单一，排障时也更清晰。

#### 9.2.2 避免人工漏提 Job

你们现在不是只有一个 Flink Job：

- `course-search-index-sync-job`
- `tool-search-index-sync-job`

后面还可能继续增加：

- `seckill-item-index-sync-job`

如果每次都手工进入容器执行两三条 `flink run`，很容易漏掉。

#### 9.2.3 提交逻辑脚本化，便于交接

提交逻辑写在脚本里后：

1. 新人知道该改哪里
2. 运维知道该执行哪个容器
3. 发版流程可重复

#### 9.2.4 更适合“更新 jar 后再次一键提交”

因为你只需要：

1. 替换 jar
2. 调整脚本里提交的 job 列表
3. 重新创建 `flink-submitter`

就能完成新的 Job 提交。

## 10. 服务器上真实的 `flink-submit-jobs.sh`

当前服务器实际挂载并执行的脚本是：

- `/opt/docker-apps/flink/flink-submit-jobs.sh`

其内容如下：

```bash
#!/bin/sh

set -e

# 这个脚本的维护方式：
# 1. 把所有 Flink 业务 Jar 放到服务器的 /home/flink/jars 目录下。
#    它们会被挂载到容器内的 /opt/flink/usrlib。
# 2. 如果要新增任务，就在脚本底部追加一行 submit_job：
#    submit_job "/opt/flink/usrlib/your-job.jar" "com.xxx.YourMainClass" "your-job-name"
# 3. 如果更新了 Jar，先替换 /home/flink/jars 下的文件，再重新创建 flink-submitter
#    或者在任务代码变化较大时直接重建整个 Flink 集群。

submit_job() {
  JAR_PATH="$1"
  MAIN_CLASS="$2"
  JOB_NAME="$3"

  for i in $(seq 1 60); do
    if flink list -m flink-jobmanager-1:30081 2>/dev/null | grep -q "$JOB_NAME"; then
      echo "$JOB_NAME already running, skip submit."
      return 0
    fi

    if [ -f "$JAR_PATH" ] && flink run -m flink-jobmanager-1:30081 -d -c "$MAIN_CLASS" "$JAR_PATH"; then
      echo "$JOB_NAME submitted."
      return 0
    fi

    sleep 5
  done

  echo "Submit $JOB_NAME failed after retries."
  return 1
}

submit_job "/opt/flink/usrlib/backstage-flink-3.9.0.jar" "com.bachstage.course.job_stream_from_kafka_to_es.CourseSearchIndexSyncJob" "course-search-index-sync-job"
submit_job "/opt/flink/usrlib/backstage-flink-3.9.0.jar" "com.bachstage.tool.job_stream_from_kafka_to_es.ToolSearchIndexSyncJob" "tool-search-index-sync-job"
```

### 10.1 这个脚本做了什么

这个脚本的行为比前面那个抽象示例更温和、更适合现网：

1. 不先强制 cancel 旧 job
2. 先 `flink list` 看 job 是否已存在
3. 如果该 job 已在运行，就跳过提交
4. 如果不存在，就尝试 `flink run`
5. 最多重试 60 次，每次间隔 5 秒

这意味着它主要适用于：

- Flink 集群刚启动，需要自动把任务补提交上去
- 某个 job 掉了，需要自动补提

它不适合做“强制覆盖式发布”，因为它不会先 cancel 老 job。

## 11. 更新 Jar 包后应该放到哪里

当前服务器上，Flink 业务 jar 必须放在：

```text
/home/flink/jars
```

因为 compose 中有这个挂载：

```yaml
- /home/flink/jars:/opt/flink/usrlib
```

所以对工具模块和课程模块来说，当前实际使用的是：

```text
/home/flink/jars/backstage-flink-3.9.0.jar
```

容器内对应路径是：

```text
/opt/flink/usrlib/backstage-flink-3.9.0.jar
```

因此：

- **更新 jar 时，应该替换宿主机的 `/home/flink/jars/backstage-flink-3.9.0.jar`**
- 不是手工改容器内文件

## 12. 如果要新增或修改要提交的 Job，改哪个脚本

要改的不是 JobManager 容器命令，而是：

- `/opt/docker-apps/flink/flink-submit-jobs.sh`

### 12.1 如果只是更新工具模块代码

通常步骤是：

1. 本地重新打包：

```bash
mvn -pl backstage-flink -am clean package -DskipTests
```

2. 把新包上传覆盖到：

```text
/home/flink/jars/backstage-flink-3.9.0.jar
```

3. 重新创建 `flink-submitter` 容器，让它再执行一遍提交脚本

### 12.2 如果要新增一个新 Job

例如未来要加：

- `SeckillItemIndexSyncJob`

那就要在脚本底部追加一行：

```bash
submit_job "/opt/flink/usrlib/backstage-flink-3.9.0.jar" "com.bachstage.seckill.job_stream_from_kafka_to_es.SeckillItemIndexSyncJob" "seckill-item-index-sync-job"
```

这就是“怎么改脚本”的关键点：

- 新增任务，不是改 compose command
- 而是往 `flink-submit-jobs.sh` 底部追加新的 `submit_job`

### 12.3 如果只是修改已有 Job 的主类或 Jar 名称

例如以后 jar 改名为：

```text
backstage-flink-4.0.0.jar
```

那需要同步改脚本里的：

```bash
submit_job "/opt/flink/usrlib/backstage-flink-4.0.0.jar" ...
```

如果主类包名变了，也要一起改。

## 13. 如何再次一键提交 Flink Job

基于当前现网设计，“一键提交”其实不是执行一个宿主机 shell，而是：

- **重新创建 `flink-submitter` 容器**

因为它的启动命令就是：

```bash
/bin/sh /opt/flink/scripts/flink-submit-jobs.sh
```

### 13.1 推荐执行方式

在服务器上进入：

```bash
cd /opt/docker-apps
```

然后执行：

```bash
docker compose rm -f flink-submitter
docker compose up flink-submitter
```

或者更常见一点：

```bash
docker compose up --force-recreate flink-submitter
```

这样会触发：

1. 重新创建 `flink-submitter`
2. 自动执行 `flink-submit-jobs.sh`
3. 自动检查 job 是否已存在
4. 不存在则自动提交

### 13.2 如果想强制重新发布工具/课程 Job

因为现网脚本本身只是“存在就跳过”，所以**强制重发版**时要先手工取消旧 job，再重新起 submitter。

流程是：

```bash
docker exec flink-jobmanager-1 flink list -a
docker exec flink-jobmanager-1 flink cancel <course-job-id>
docker exec flink-jobmanager-1 flink cancel <tool-job-id>
docker compose up --force-recreate flink-submitter
```

这样 submitter 才会重新把 job 提上去。

## 14. 为什么文档里要写 `flink-submitter`

因为你们当前现网不是“纯手工模式”，而是已经有了一个很明确的提交层：

- compose 中的 `flink-submitter`
- 宿主机脚本 `/opt/docker-apps/flink/flink-submit-jobs.sh`

如果文档只写抽象脚本示例，而不写现网的：

- submitter 容器
- 实际脚本路径
- 实际挂载关系
- 实际一键执行方式

那后面接手的人看完仍然不知道：

1. Jar 该放哪
2. 脚本该改哪
3. 是重启 JobManager 还是重建 submitter
4. 为什么 compose 里多了一个 submitter 服务

所以文档必须把这一层写清楚。

## 10. 更新 Jar 后要放到哪里

更新 jar 后，服务器上应该放到：

```text
/home/flink/jars/backstage-flink-3.9.0.jar
```

因为它会挂载进容器：

```text
/opt/flink/usrlib/backstage-flink-3.9.0.jar
```

你只改容器内路径没有意义，因为容器内文件来自宿主机挂载目录。

## 11. 要改哪个脚本，怎么改，才能再次一键提交 Flink Job

如果你们现在还没有这类脚本，建议新增：

```text
/home/flink/bin/redeploy-flink-jobs.sh
```

如果已经有旧脚本，比如只提交课程 Job，那么需要改成：

1. `JAR_PATH` 改成当前统一包路径
2. 增加工具模块主类：

```bash
TOOL_MAIN="com.bachstage.tool.job_stream_from_kafka_to_es.ToolSearchIndexSyncJob"
```

3. 在重启后补提工具 Job：

```bash
docker exec "$CONTAINER" flink run -d -c "$TOOL_MAIN" /opt/flink/usrlib/backstage-flink-3.9.0.jar
```

4. 如果还要统一管理课程和工具，最好把二者都写进同一个脚本

这样之后每次只需要：

1. 本地重新打包 `backstage-flink-3.9.0.jar`
2. 上传覆盖到 `/home/flink/jars/backstage-flink-3.9.0.jar`
3. 服务器执行：

```bash
bash /home/flink/bin/redeploy-flink-jobs.sh
```

即可一键完成：

- cancel 旧 job
- restart Flink
- submit 新 job
- 验证运行状态

## 12. 推荐后续优化

### 12.1 显式给 Flink 容器加时区

建议在 `/opt/docker-apps/docker-compose.yml` 的 Flink 服务中加：

```yaml
environment:
  - TZ=Asia/Shanghai
```

避免 `ZoneId.systemDefault()` 造成时间戳偏移。

### 12.2 给工具模块单独提交脚本

如果未来工具模块发布频率高于课程模块，可以单独拆一个脚本：

```text
/home/flink/bin/redeploy-tool-flink-job.sh
```

只提交：

- `ToolSearchIndexSyncJob`

### 12.3 在脚本里加健康检查

例如：

```bash
curl -sf http://127.0.0.1:30081/overview
```

确认 JobManager REST 端口可用后再提交。

## 13. 结论

工具模块 Flink 任务的核心设计是：

1. Kafka 统一 Topic 输入
2. 按 `eventType` 路由到：
   - 全量 upsert
   - 审核 partial update
   - delete
3. 时间统一兼容：
   - Java / MySQL 主格式：`yyyy-MM-dd HH:mm:ss`
   - Flink 写 ES 时统一转 `epoch_millis`
   - ES mapping 应兼容字符串和毫秒
4. 服务器部署上，真正要替换的是：
   - `/home/flink/jars/backstage-flink-3.9.0.jar`
5. 真正生效的 compose 是：
   - `/opt/docker-apps/docker-compose.yml`
6. 为了稳定重复发布，应该把：
   - 替换 jar
   - cancel 旧 job
   - restart Flink
   - submit 新 job

统一收敛到一个一键脚本里。

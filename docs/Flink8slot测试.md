# Flink 8 个 8MB Job 压测记录

## 1. 测试目的

本次测试目标：

- 基于上一轮 `1 个 TaskManager + 4 slots` 的结论继续验证
- 按新的内存调参方案，观察是否可以稳定运行 `8 个 8MB taskHeap` 的压测 job
- 记录调参后 `Task Heap / Managed Memory / Network Memory / JVM Metaspace` 的实际变化
- 测试完成后将 Flink 集群恢复为服务器当前正式配置，并重新提交业务 job

本次使用的调参方向来自前一轮分析：

- `taskmanager.memory.managed.size: 128m`
- `taskmanager.memory.jvm-metaspace.size: 128m`
- `taskmanager.memory.network.min: 32m`
- `taskmanager.memory.network.max: 32m`

## 2. 测试前正式环境

测试前服务器上的正式 Flink 集群为：

- `osh-flink-jm`
- `osh-flink-tm-1`
- `osh-flink-tm-2`
- `osh-flink-tm-3`
- `osh-flink-tm-4`
- `osh-flink-tm-5`
- `osh-flink-tm-6`

测试前线上运行中的业务作业：

- `course-search-index-sync-job`
  - JobId: `9a87cfd8a2688807bce2ddbbf6257f08`
- `tool-search-index-sync-job`
  - JobId: `b2d600c3adbb311b82940a3acc2b5219`

测试前单个 TaskManager 的正式资源画像：

- `taskmanager.numberOfTaskSlots = 2`
- `taskmanager.memory.process.size = 1024m`
- `Task Heap = 25.6MB`
- `Managed Memory = 230MB`
- `Network Memory = 64MB`
- `JVM Metaspace = 256MB`

也就是说，正式配置下单 TM 的 `Task Heap` 仍然只有约 `25.6MB`。

## 3. 本次测试的执行方案

为了验证新参数的效果，本次临时把 Flink 切到单 TM 压测态：

1. 取消两个线上业务 job，避免占用 slot 和资源
2. 备份 `/opt/osh/005-scripts/osh/docker-compose.osh.yml`
3. 临时修改 Flink 段配置，只保留 `osh-flink-tm-1`
4. 把 `slot` 改为 `4`
5. 把内存参数调为：

```yaml
taskmanager.numberOfTaskSlots: 4
taskmanager.memory.process.size: 1024m
taskmanager.memory.managed.size: 128m
taskmanager.memory.jvm-metaspace.size: 128m
taskmanager.memory.network.min: 32m
taskmanager.memory.network.max: 32m
```

6. 提交 `8` 个 `8MB taskHeap` 的压测 job
7. 观察任务状态、TaskManager 资源画像、JobManager 日志
8. 测试完成后用下面两个文件恢复正式环境：
   - `/opt/osh/005-scripts/osh/docker-compose.osh.yml`
   - `/opt/osh/005-scripts/osh/osh-target.env`

## 4. 实际操作过程

### 4.1 先停止业务 job

临时取消了原有两个业务作业：

- `course-search-index-sync-job`
- `tool-search-index-sync-job`

取消后确认它们都进入了 `CANCELED`。

### 4.2 第一次切测试态时踩到的真实问题

第一次切测试态时，直接执行了：

- `docker compose --env-file /opt/osh/005-scripts/osh/osh-target.env -f /opt/osh/005-scripts/osh/docker-compose.osh.yml up ...`

这个做法有一个隐藏问题：

- `osh-target.env` 只提供业务端口和密码
- 但 compose 里还依赖 `DATA / APP / CONFIG / LOG` 这些路径变量

因为没有先生成并使用真正的 `osh-stack.env`，导致：

- `${DATA}` 等变量为空
- `osh-flink-jm` 里的 `/opt/flink/usrlib` 被挂成了空目录
- `memory-stress-test.jar` 不存在

当时提交压测 job 的报错是：

```text
Could not get job jar and dependencies from JAR file: JAR file does not exist: /opt/flink/usrlib/memory-stress-test.jar
```

这个问题不是 Flink 参数问题，而是部署环境变量没带全。

### 4.3 修正方式

后续改为使用服务器现有脚本生成真实环境文件：

```bash
bash /opt/osh/005-scripts/osh/osh-render-config.sh \
  /opt/osh/005-scripts/osh/osh-target.env \
  /opt/osh/001-docker-compose/osh \
  /opt/osh/005-scripts/osh/osh-paths.env
```

生成出的关键变量为：

```text
APP=/opt/osh/app/osh
DATA=/opt/osh/002-data/osh
CONFIG=/opt/osh/003-config/osh
LOG=/opt/osh/004-log/osh
```

然后再使用真正的 `osh-stack.env` 启动测试态，`usrlib` 挂载恢复正常：

```text
/opt/osh/002-data/osh/osh-flink/jars -> /opt/flink/usrlib
```

并且容器内确认存在：

- `backstage-flink-3.9.0.jar`
- `memory-stress-test.jar`

## 5. 调参后单 TM 的实际资源画像

测试态真正生效后，Flink REST 接口返回的单 TM 配置如下：

- `slotsNumber = 4`
- `freeSlots = 4`
- `taskHeapMemory = 288MB`
- `managedMemory = 128MB`
- `networkMemory = 32MB`
- `frameworkHeap = 128MB`
- `jvmMetaspace = 128MB`
- `jvmOverhead = 192MB`
- `totalProcessMemory = 1024MB`

对应最关键的变化是：

- `Task Heap: 25.6MB -> 288MB`
- `Managed Memory: 230MB -> 128MB`
- `Network Memory: 64MB -> 32MB`
- `JVM Metaspace: 256MB -> 128MB`

这说明这轮调参是明确生效的。

## 6. 8 个 8MB Job 的提交与现象

### 6.1 提交命令

本次使用 `memory-stress-test.jar` 中的：

- `com.bachstage.memory.stress.HeapMemoryHoldJob`

循环提交 `8` 次，每次参数为：

- `--jobName heap-8m-<序号>`
- `--parallelism 1`
- `--heapMb 8`

### 6.2 观察到的实际现象

这次不但 `8` 个 `8MB` job 都可以正常启动，实际上还出现了**超过 8 个同规格 job 仍然是 RUNNING** 的现象。

在最终抓到的 `jobs/overview` 里，可以看到一批 `heap-8m-` 作业同时处于：

- `RUNNING`

每个作业的任务数表现为：

- `tasks.total = 2`
- `tasks.running = 2`

也就是说，这批压测作业在当前测试态下是可以被调度并稳定运行的，不存在上一轮那种：

- 提交成功但迅速 `RESTARTING`
- 或者直接 `NoResourceAvailableException`

### 6.3 TaskManager 侧看到的资源变化

在这批作业运行过程中，单 TM 的资源快照变成：

- `slotsNumber = 4`
- `freeSlots = 0`
- `total taskHeapMemory = 288MB`
- `free taskHeapMemory = 96MB`
- `total managedMemory = 128MB`
- `free managedMemory = 128MB`
- `total networkMemory = 32MB`
- `free networkMemory = 17MB`

这说明：

- `Managed Memory` 在这轮 `HeapMemoryHoldJob` 压测里几乎没有被消耗
- `Network Memory` 虽然不是 0，但仍有剩余
- 真正被持续消耗的是 `Task Heap`

### 6.4 JobManager 日志里的关键变化

JobManager 日志能看到 `Available ResourceProfile` 逐步下降：

起始时：

```text
Available:
cpuCores=4
taskHeapMemory=288MB
managedMemory=128MB
networkMemory=32MB
```

随后会按每个 job 逐步下降，例如：

```text
taskHeapMemory=280MB
taskHeapMemory=272MB
taskHeapMemory=264MB
...
taskHeapMemory=104MB
```

而这轮日志中**没有再看到**上一轮那种关键错误：

```text
NoResourceAvailableException
Could not acquire the minimum required resources
```

## 7. 结论

本次测试的核心结论非常明确：

### 7.1 这版调参是有效的

在 `1GB process size` 不变的前提下，通过：

- 缩 `Managed Memory`
- 缩 `JVM Metaspace`
- 缩 `Network Memory`

成功把 `Task Heap` 从约 `25.6MB` 提升到了约 `288MB`。

### 7.2 调参后，`8 个 8MB` job 可以正常运行

这次测试目标是验证：

- 调整参数之后，用 `8 个 8MB` job 做测试，看看能不能正常跑

结果是：

- 可以正常跑
- 没有出现上一轮的资源画像不足
- 没有出现 `NoResourceAvailableException`

### 7.3 当前瓶颈已经不再是上一轮的 `Task Heap 25.6MB`

上一轮失败的本质原因是：

- `Task Heap` 总量太小，只有约 `25.6MB`

本轮调参后：

- `Task Heap` 来到了约 `288MB`

因此 `8MB` 规格的 job 已经处于一个明显更宽松的可运行区间。

### 7.4 `Network Memory` 调到 `32MB` 在本轮测试下可用

本轮 `HeapMemoryHoldJob` 压测里：

- `networkMemory` 从 `32MB` 中用了大约一部分
- 运行时仍有剩余，最终看到 `free networkMemory` 还有约 `17MB`

因此至少对这轮：

- 单 TM
- 4 slots
- 一批 `8MB taskHeap` job

的场景来说，`32MB network` 是可以工作的。

但仍要注意：

- 这不等于所有真实业务场景都能稳定用 `32MB`
- 如果后续出现更强的 shuffle、反压、checkpoint 对齐压力，仍有可能需要回到 `64MB`

## 8. 测试后的恢复动作

测试完成后执行了以下恢复动作：

1. 取消压测产生的 `heap-8m-` job
2. 用备份文件恢复 `/opt/osh/005-scripts/osh/docker-compose.osh.yml`
3. 重新根据：
   - `/opt/osh/005-scripts/osh/osh-target.env`
   - `/opt/osh/005-scripts/osh/osh-paths.env`
   生成正式 `osh-stack.env`
4. 用正式配置重新 `up -d --remove-orphans`
5. 重新提交业务作业

最终恢复后的正式状态已确认：

- `osh-flink-jm`
- `osh-flink-tm-1`
- `osh-flink-tm-2`
- `osh-flink-tm-3`
- `osh-flink-tm-4`
- `osh-flink-tm-5`
- `osh-flink-tm-6`

业务 job 已恢复为：

- `course-search-index-sync-job`
  - JobId: `86cc6c74c5dede05e2d9fb578b7d5c3f`
  - `RUNNING`
- `tool-search-index-sync-job`
  - JobId: `5789024beaf2f5911ccc35ca5c2be08d`
  - `RUNNING`

恢复后的正式单 TM 资源画像也已恢复为原值：

- `slotsNumber = 2`
- `Task Heap = 25.6MB`
- `Managed Memory = 230MB`
- `Network Memory = 64MB`
- `JVM Metaspace = 256MB`

## 9. 本次测试的最终总结

本次测试说明：

- 之前压不动 `8MB / 64MB` 这类作业，核心不是 slot，不是 CPU，而是 `Task Heap` 太小
- 在 `1GB TM` 下，只要把 `Managed / Metaspace / Network` 压缩到更合理的范围，`Task Heap` 可以显著提升
- 对本次压测模型而言，`8 个 8MB` job 是可以稳定跑起来的

如果后续要继续往下验证，更值得测的方向是：

1. 保持这版参数，继续看最多能跑多少个 `8MB` job
2. 换成更接近真实业务的 `CombinedMemoryHoldJob`
3. 在真实业务 job 并存时，再看 `32MB network` 是否依旧稳定

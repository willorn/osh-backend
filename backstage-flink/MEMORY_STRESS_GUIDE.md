# Flink 内存压测任务说明

## 目标

这个目录下新增了 3 个可提交到 Flink 的压测入口，用来观察：

- 单个 job / 单个 subtask 声明多少 `Task Heap`
- 单个 job / 单个 subtask 声明多少 `Managed Memory`
- 当前 TaskManager 资源下最多还能塞下几个同规格 job

## 入口类

- `com.bachstage.memory.stress.HeapMemoryHoldJob`
- `com.bachstage.memory.stress.ManagedMemoryProfileJob`
- `com.bachstage.memory.stress.CombinedMemoryHoldJob`

## 模式说明

### 1. HeapMemoryHoldJob

- 使用 `SlotSharingGroup` 声明 `Task Heap`
- 同时在 Source 的 `open()` 中持有指定大小 `byte[]`
- 更适合验证 JVM Heap 相关表现

### 2. ManagedMemoryProfileJob

- 使用 `SlotSharingGroup` 声明 `Managed Memory`
- 不额外持有大块 `byte[]`
- 更适合验证 Flink 调度和资源切 slot 的结果

说明：
这个模式更接近 Flink 的资源模型，不等于直接在 JVM 堆里真实持有同等大小对象。

### 3. CombinedMemoryHoldJob

- 同时声明 `Task Heap` 和 `Managed Memory`
- 并在 Heap 中实际持有 `byte[]`
- 更接近真实业务算子既吃堆内存又声明资源画像的场景

## 常用参数

- `--parallelism`：并发度，默认 `1`
- `--cpuCores`：每个 slot sharing group 声明的 CPU，默认 `0.1`
- `--heartbeatIntervalMs`：Source 心跳间隔，默认 `5000`
- `--jobName`：任务名
- `--slotSharingGroup`：slot sharing group 名称
- `--taskHeapMb`：声明的 task heap 大小
- `--taskOffHeapMb`：声明的 task off-heap 大小
- `--managedMb`：声明的 managed memory 大小
- `--holdHeapMb`：实际持有的 JVM Heap 大小
- `--heapMb`：`taskHeapMb` 与 `holdHeapMb` 的快捷参数

## 提交示例

也可以直接使用仓库里的脚本：

```bash
./submit_flink_job.sh \
  -c com.bachstage.memory.stress.HeapMemoryHoldJob \
  -p 1 \
  -n heap-64m \
  --tm-jvm-opts "-Xms256m -Xmx256m" \
  -- --heapMb 64 --parallelism 1
```

### 64MB Heap 任务

```bash
flink run -c com.bachstage.memory.stress.HeapMemoryHoldJob backstage-flink-3.9.0.jar \
  --jobName heap-64m \
  --parallelism 1 \
  --heapMb 64
```

### 64MB Managed Memory 画像任务

```bash
flink run -c com.bachstage.memory.stress.ManagedMemoryProfileJob backstage-flink-3.9.0.jar \
  --jobName managed-64m \
  --parallelism 1 \
  --managedMb 64 \
  --taskHeapMb 64
```

### 64MB Heap + 128MB Managed 组合任务

```bash
flink run -c com.bachstage.memory.stress.CombinedMemoryHoldJob backstage-flink-3.9.0.jar \
  --jobName combined-64m-128m \
  --parallelism 1 \
  --heapMb 64 \
  --managedMb 128
```

## 提交脚本能力

新增脚本：`backstage-flink/submit_flink_job.sh`

支持：

- 设置 Flink 提交并行度：`-p 2`
  - 不传时默认按 `1` 提交
- 设置 REST 地址：`-r 127.0.0.1:30081`
- 透传动态配置：`-D taskmanager.numberOfTaskSlots=4`
- 从 savepoint 恢复：`--from-savepoint`
- 允许跳过未恢复状态：`--allow-non-restored-state`
- 指定恢复模式：`--restore-mode`
- 增加 classpath：`--classpath`
- 设置 JVM 参数：
  - `--all-jvm-opts`
  - `--jm-jvm-opts`
  - `--tm-jvm-opts`
- 通过 `--` 继续传作业自己的参数

示例：

```bash
./submit_flink_job.sh \
  -c com.bachstage.memory.stress.ManagedMemoryProfileJob \
  -r 127.0.0.1:30081 \
  --from-savepoint file:///tmp/savepoint-xxx \
  --allow-non-restored-state \
  --restore-mode CLAIM \
  --tm-jvm-opts "-Xms256m -Xmx256m" \
  -- --managedMb 64 --taskHeapMb 64
```

## 如何观察

提交后重点看：

1. `TaskManager -> Metrics -> Resources`
   - `Task Heap memory`
   - `Managed memory`
   - `CPU`

2. `TaskManager -> Metrics -> JVM Heap/Non-Heap Memory`
   - Heap `Used / Maximum`

3. `Free/All Slots`
   - 看 slot 是否被继续切分或调度失败

## 压测建议

建议按下面顺序试：

1. 单并发 `64MB`
2. 单并发 `128MB`
3. 单并发 `256MB`
4. 多次重复提交同规格 job
5. 观察什么时候开始无法调度或资源明显不足

这样比较容易看出当前 TaskManager 在你现有配置下的实际承载边界。

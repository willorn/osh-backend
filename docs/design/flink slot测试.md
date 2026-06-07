# Flink Slot 测试记录

## 1. 测试目的

本次测试目标：

- 验证当前 Flink 集群在 `1 个 TaskManager + 4 slots` 条件下，是否可以承载 `4 个 64MB` 级别的压测作业
- 分别验证 `heap`、`managed`、`combined` 三种模式
- 观察失败时到底是：
  - slot 不够
  - JVM 内存不够
  - Flink 资源画像不匹配
  - 还是作业代码本身有问题

## 2. 测试前环境

默认服务器：`default`

测试前线上 Flink 集群状态：

- `flink-jobmanager-1`
- `flink-taskmanager-1`
- `flink-taskmanager-2`

原有线上运行作业：

- `course-search-index-sync-job`
  - JobId: `4984c7559960e68900094eabc71ac215`
- `tool-search-index-sync-job`
  - JobId: `1b50aba82b8f3aef299e51b66873e763`

原始 TaskManager 配置：

- `2 个 TaskManager`
- 每个 `taskmanager.numberOfTaskSlots: 2`
- 每个 `taskmanager.memory.process.size: 1024m`

## 3. 本次做过的操作

### 3.1 代码侧新增内容

在 `backstage-flink` 模块新增了内存压测作业：

- `com.bachstage.memory.stress.HeapMemoryHoldJob`
- `com.bachstage.memory.stress.ManagedMemoryProfileJob`
- `com.bachstage.memory.stress.CombinedMemoryHoldJob`

同时新增通用提交脚本：

- [submit_flink_job.sh](/Users/whiskey_liu/IdeaProjects/osh-backend/backstage-flink/submit_flink_job.sh:1)

脚本支持：

- 设置并行度 `-p`
- 透传 Flink 动态配置 `-D`
- 设置 JVM 参数：
  - `--all-jvm-opts`
  - `--jm-jvm-opts`
  - `--tm-jvm-opts`

### 3.2 服务器侧操作

为了做单 TM 压测，临时调整了服务器：

1. 备份 `/opt/docker-apps/docker-compose.yml`
2. 将 `flink-taskmanager-1` 的 slot 改为 `4`
3. 临时移除 `flink-taskmanager-2`
4. 重启 Flink 集群

调整后确认状态：

- 只剩 `1 个 TaskManager`
- `slotsNumber = 4`
- `freeSlots = 4`

### 3.3 为测试让出资源

为了让 4 个压测 job 有机会完整占满 4 个 slot，临时停止了原有线上作业：

- cancel `course-search-index-sync-job`
- cancel `tool-search-index-sync-job`

停止后确认：

- `No running jobs`
- `TaskManager freeSlots = 4`

## 4. 第一次提交测试的实际情况

第一次提交 4 个 `64MB` job 时，所有作业在提交阶段直接失败。

提交的 4 个作业分别是：

1. `HeapMemoryHoldJob`
2. `ManagedMemoryProfileJob`
3. `CombinedMemoryHoldJob`
4. `HeapMemoryHoldJob`

第一次失败的真实原因不是 slot，也不是内存，而是代码问题：

```text
com.bachstage.memory.stress.MemoryStressJobOptions is not serializable
```

定位结果：

- `MemoryStressSource` 持有了 `MemoryStressJobOptions`
- Flink 在提交时会做 closure clean
- `MemoryStressJobOptions` 没有实现 `Serializable`

对应修复：

- 让 `MemoryStressJobOptions implements Serializable`

这是一次真实测试中先暴露出来的代码缺陷，不是资源限制问题。

## 5. 第二次提交测试的实际情况

修复序列化问题后，重新打包并上传了小 jar：

- `/home/flink/jars/memory-stress-test.jar`
- 容器内路径：`/opt/flink/usrlib/memory-stress-test.jar`

然后重新提交 4 个 `64MB` job：

1. heap
   - JobId: `365b9a40e14508a042e2ce514136bab4`
2. managed
   - JobId: `a66a9ff9368cea6a6fde8b9af5078011`
3. combined
   - JobId: `8d54b0d114c3519284a9ab0fb7417ac0`
4. heap
   - JobId: `f7e369de25d5d6e1f98b83b6278b2587`

这一次 4 个作业都能成功提交，但很快全部进入 `RESTARTING`。

## 6. 最终失败原因

最终根因已经从 JobManager 日志里确认：

```text
NoResourceAvailableException: Could not acquire the minimum required resources.
```

更关键的是资源画像对比：

### 6.1 压测作业申请的资源

`managed` / `combined` 模式申请资源：

```text
cpuCores=0.1
taskHeapMemory=64MB
managedMemory=64MB
networkMemory=640KB
```

`heap` 模式申请资源：

```text
cpuCores=0.1
taskHeapMemory=64MB
managedMemory=0
networkMemory=640KB
```

### 6.2 当前单个 TaskManager 实际可提供资源

JobManager 日志明确显示：

```text
Available:
cpuCores=4
taskHeapMemory=25.600mb
managedMemory=230.400mb
networkMemory=64.000mb
```

### 6.3 结论

问题核心不是 slot 数，而是：

**单个 TaskManager 的 `taskHeapMemory` 只有 `25.6MB`，远小于每个压测 job 申请的 `64MB taskHeapMemory`。**

所以：

- 即使有 `4 slots`
- 即使 `freeSlots = 4`
- 也依然无法调度一个要求 `taskHeapMemory=64MB` 的 job

换句话说：

**slot 空闲不代表资源足够。**

当前这个 TM 的真实瓶颈是：

- `Task Heap memory` 太小

不是：

- CPU
- Managed memory
- slot 数量

## 7. 这次测试说明了什么

这次测试有几个非常明确的结论：

### 7.1 `slot` 只是并发配额，不是内存额度

你把 slot 从 `2` 改到 `4`，并不代表每个 job 自动能拿到足够内存。

真正决定能不能调度的是：

- `taskHeapMemory`
- `managedMemory`
- `networkMemory`
- `cpuCores`

### 7.2 当前这套 TM 配置不适合 64MB taskHeap 画像的 job

当前单 TM 实际可供任务使用的 task heap 只有：

```text
25.6MB
```

所以：

- `64MB heap` 不行
- `64MB managed + 64MB taskHeap` 更不行

### 7.3 你前面在 UI 看到的结论是对的

Flink UI 里看到的：

- `Task Heap memory: 25 / 25 MB`
- `Managed memory: 230 / 230 MB`

这不是“运行时随便看看”，而是**直接影响调度能否成功**。

本次实测已经验证：

- 当 job 画像里声明 `taskHeapMemory=64MB`
- 但 TM 实际只有 `25.6MB taskHeap`
- Flink 根本不会把任务派上去

## 8. 本次测试的最终实验结论

在当前测试配置下：

- `1 个 TaskManager`
- `4 slots`
- `taskmanager.memory.process.size = 1024m`

尝试运行 `4 个 64MB job`

结果：

- 4 个 job 都可以提交
- 但都无法稳定运行
- 最终原因是 **TM 可提供的 task heap 只有 25.6MB，无法满足每个 job 申请的 64MB task heap**

所以当前结论不是：

- “4 slots 能跑 4 个 64MB job”

而是：

- **当前这台 1GB TM 即使开到 4 slots，也跑不动一个声明 `64MB taskHeap` 的压测 job**

## 9. 服务器脚本状态

服务器上已经放好了提交脚本：

- 宿主机路径：`/home/flink/jars/submit_flink_job.sh`
- 容器内路径：`/opt/flink/usrlib/submit_flink_job.sh`

同时也放好了压测 jar：

- 宿主机路径：`/home/flink/jars/memory-stress-test.jar`
- 容器内路径：`/opt/flink/usrlib/memory-stress-test.jar`

注意：

服务器宿主机没有直接安装 `flink` 命令，所以如果在宿主机直接执行脚本，需要给环境变量：

```bash
FLINK_BIN="docker exec flink-jobmanager-1 /opt/flink/bin/flink"
```

本地脚本已经支持这种命令前缀形式。

## 10. 测试结束后的恢复动作

测试结束后，已经恢复线上环境：

### 10.1 Flink 集群恢复

- 恢复 `/opt/docker-apps/docker-compose.yml`
- 恢复为 `2 个 TaskManager`
- 每个 `2 slots`

确认恢复后状态：

- `flink-taskmanager-1`：`2 slots`
- `flink-taskmanager-2`：`2 slots`

### 10.2 原线上作业恢复

重新提交并确认运行：

- `course-search-index-sync-job`
  - 新 JobId: `4d57cd8e62ca669f2c0e62388e3ed251`
- `tool-search-index-sync-job`
  - 新 JobId: `f67503eb6436ef820f805a130e80bfc2`

当前确认结果：

- 两个作业都处于 `RUNNING`

## 11. 下一步建议

如果要继续验证“单 TM 下到底能跑几个 64MB job”，建议下一轮这样改：

1. 增大 `taskmanager.memory.process.size`
   - 比如先提高到 `2048m` 或 `3072m`
2. 同时观察 `Task Heap memory` 是否随之提升
3. 再提交：
   - `heap 32MB`
   - `heap 64MB`
   - `managed 64MB`
   - `combined 32MB + 64MB`
4. 不要只看 `freeSlots`
   - 重点看 `taskHeapMemory` 是否真的够

如果继续做下一轮，优先建议：

- 先做 `32MB` taskHeap 画像
- 再逐步抬到 `64MB`

这样更容易测出当前 TM 的真实上限。

## 12. 补充实验：32MB 与 16MB

在完成 `64MB` 测试后，又额外补了两轮：

- `32MB`
- `16MB`

测试方法保持一致：

1. 再次切成 `1 个 TaskManager + 4 slots`
2. 临时停掉两个正式作业
3. 提交 4 个压测 job
4. 观察 job 状态、TaskManager 资源、JobManager 日志
5. 测试完成后恢复线上环境

### 12.1 32MB 测试

提交的 4 个作业：

1. heap 32MB
   - JobId: `031964225f760079534d10aa566c0c13`
2. managed 32MB
   - JobId: `e21d7e504aaa3688710c58ec2bc990a6`
3. combined 32MB
   - JobId: `65787cb7e1e1bf4c35712baaf191dc1d`
4. heap 32MB
   - JobId: `2c276c44b3e7a8a90e2fa7027b57cd2c`

实际结果：

- 4 个 job 都能成功提交
- 但全部进入 `RESTARTING`
- 最终没有一个稳定运行

JobManager 日志里的关键信息：

```text
ResourceRequirement:
taskHeapMemory = 32MB

Current TaskManager Available:
taskHeapMemory = 25.6MB
```

结论：

- `32MB` 仍然大于当前单个 TM 可提供的 `25.6MB taskHeap`
- 所以 32MB 这一档和 64MB 一样，连第 1 个 job 都无法调度成功

### 12.2 16MB 测试

提交的 4 个作业：

1. heap 16MB
   - JobId: `478d37f4a3d1404ac9db85bc8c7131f0`
2. managed 16MB
   - JobId: `7efc81fbfbd4246bc4298740ea803759`
3. combined 16MB
   - JobId: `3171a4d3ff27b8fb1a6b170eee0394e6`
4. heap 16MB
   - JobId: `8d084ed6ad4b047702ed6f048b30da65`

实际结果：

- 第 1 个 `heap-16MB` 成功运行
- 后面 3 个作业都进入 `RESTARTING`

当时 TaskManager 的实时资源快照：

```text
slotsNumber = 4
freeSlots = 3
freeResource:
  cpuCores = 3.9
  taskHeapMemory = 9.6MB
  managedMemory = 230MB
  networkMemory = 63MB
```

这说明：

- 第 1 个 `heap-16MB` job 成功占用了 1 个 slot
- 它同时也吃掉了约 `16MB taskHeap`
- 所以原本 `25.6MB` 的 taskHeap 只剩下约 `9.6MB`

后续作业失败的直接原因也很明确：

```text
新 job 需要:
taskHeapMemory = 16MB

但当前剩余:
taskHeapMemory = 9.6MB
```

所以第 2、3、4 个作业都无法再被调度。

### 12.3 补充实验结论

把 `64MB / 32MB / 16MB` 三轮一起看，当前这台 `1GB TM` 的结论已经很清楚：

1. `64MB`
   - 0 个能稳定运行
2. `32MB`
   - 0 个能稳定运行
3. `16MB`
   - 只有第 1 个 heap 作业能稳定跑起来
   - 第 2 个开始就因为剩余 `taskHeapMemory` 不足而失败

也就是说：

**当前 `taskmanager.memory.process.size = 1024m` 的这台 TM，在 Flink 资源画像层面，能承受的单 job taskHeap 已经非常有限。**

更准确地说：

- 当前 `Task Heap` 总量只有 `25.6MB`
- 所以：
  - `> 25.6MB` 的 taskHeap 画像一个都跑不起来
  - `16MB` 只能支撑 1 个

### 12.4 这轮补充实验带来的更准确判断

之前我们知道：

- `64MB` 不行

现在进一步知道：

- `32MB` 也不行
- `16MB` 只能跑 1 个

因此当前这套单 TM 资源的实际边界可以粗略归纳为：

```text
可用 taskHeap 总量约 25.6MB
```

所以后面如果还要继续测“最多能跑几个”，建议把档位继续细化成：

- `8MB`
- `12MB`
- `16MB`
- `20MB`

这样会更容易测出精确拐点。

## 13. 第二轮实验后的恢复状态

补充实验完成后，已经再次恢复线上环境：

### 13.1 集群恢复

- 恢复为 `2 个 TaskManager`
- 每个 `2 slots`

恢复后确认：

- `TaskManager-1`：`2 slots`
- `TaskManager-2`：`2 slots`

### 13.2 正式作业恢复

重新提交并确认运行：

- `course-search-index-sync-job`
  - 新 JobId: `faf404d0dbf6ea7dbfd0e8e6bb2db2a2`
- `tool-search-index-sync-job`
  - 新 JobId: `b6aa645ac23d8712d410844bbe167f56`

当前确认状态：

- 两个作业都为 `RUNNING`

## 14. 补充实验：8MB

你提出了一个很自然的问题：

> 既然当前 `Task Heap` 总量只有约 `25.6MB`，是不是意味着只能起 `2 个 8MB` 的？

这轮专门做了 `8MB` 的实测，用来回答这个问题。

### 14.1 测试方式

测试方法和前几轮一致：

1. 切成 `1 个 TaskManager + 4 slots`
2. 临时停掉正式作业
3. 连续提交 4 个 `8MB` 画像的压测 job
4. 观察每一步后的 `freeSlots`、`freeResource` 和 JobManager 日志

提交的 4 个作业：

1. heap 8MB
   - JobId: `257419e42496ffee6302fd4ac0e919e5`
2. managed 8MB
   - JobId: `d4fdc67bdc92a6cbde7a27b41feb5864`
3. combined 8MB
   - JobId: `5ab161e1c8f640abebf9ed65cf82c17b`
4. heap 8MB
   - JobId: `371edc8c709001e71da230629ccf6b71`

### 14.2 实际结果

4 个作业提交后的真实状态：

- 第 1 个：`RUNNING`
- 第 2 个：`RUNNING`
- 第 3 个：`RUNNING`
- 第 4 个：`RESTARTING`

也就是说：

**不是只能起 2 个 8MB，而是能起 3 个，第 4 个起不来。**

### 14.3 当时的 TaskManager 资源快照

当 3 个作业成功跑起来后，TaskManager 快照是：

```text
slotsNumber = 4
freeSlots = 1

freeResource:
  cpuCores = 3.7
  taskHeapMemory = 1.6MB
  managedMemory = 214MB
  networkMemory = 62MB
```

这说明：

- 3 个 8MB 作业已经成功占掉了 3 个 slot
- 同时也把 `taskHeapMemory` 从 `25.6MB` 消耗到了只剩 `1.6MB`

### 14.4 第 4 个为什么失败

第 4 个作业需要的资源画像是：

```text
taskHeapMemory = 8MB
```

而它尝试调度时，TaskManager 剩余资源已经变成：

```text
taskHeapMemory = 1.6MB
```

因此第 4 个失败的直接原因是：

```text
1.6MB < 8MB
```

JobManager 日志里对应的判断也是：

```text
Could not acquire the minimum required resources.
NoResourceAvailableException
```

### 14.5 这轮 8MB 实验的结论

这轮实验回答了“是不是只能起 2 个 8MB”的问题：

- 不是 2 个
- 是 **3 个能跑，第 4 个失败**

更准确地说，当前这台单 TM 的 taskHeap 预算大约是：

```text
25.6MB
```

所以：

- `8MB x 3 = 24MB`，还能放下
- `8MB x 4 = 32MB`，就超过上限了

### 14.6 现在可以得出的更完整结论

把 `64MB / 32MB / 16MB / 8MB` 四轮一起看：

1. `64MB`
   - 0 个能稳定运行
2. `32MB`
   - 0 个能稳定运行
3. `16MB`
   - 1 个能稳定运行
4. `8MB`
   - 3 个能稳定运行
   - 第 4 个失败

这说明当前单 TM 的真实 taskHeap 容量边界，和 `25.6MB` 这个数高度一致。

## 15. 第三轮实验后的恢复状态

8MB 测试完成后，已再次恢复线上环境：

### 15.1 集群恢复

- 恢复为 `2 个 TaskManager`
- 每个 `2 slots`

### 15.2 正式作业恢复

重新提交并确认运行：

- `course-search-index-sync-job`
  - 新 JobId: `c195a9ee69c37cd7efedf5fd40f44f5e`
- `tool-search-index-sync-job`
  - 新 JobId: `a20556f4f079c5b2a8f574b45e83d3a1`

当前确认状态：

- 两个作业都为 `RUNNING`

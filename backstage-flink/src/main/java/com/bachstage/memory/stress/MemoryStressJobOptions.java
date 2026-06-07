package com.bachstage.memory.stress;

import org.apache.flink.api.common.operators.SlotSharingGroup;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.MemorySize;

import java.io.Serializable;

/**
 * 内存压测任务参数
 */
public final class MemoryStressJobOptions implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_PARALLELISM = 1;
    private static final int DEFAULT_HEARTBEAT_INTERVAL_MS = 5000;
    private static final double DEFAULT_CPU_CORES = 0.1D;
    private static final int DEFAULT_MANAGED_TASK_HEAP_MB = 64;
    private static final String DEFAULT_JOB_NAME_PREFIX = "memory-stress-job";
    private static final String DEFAULT_SLOT_SHARING_GROUP = "memory-stress";

    private final MemoryStressMode mode;
    private final String jobName;
    private final int parallelism;
    private final long heartbeatIntervalMs;
    private final double cpuCores;
    private final int taskHeapMb;
    private final int taskOffHeapMb;
    private final int managedMb;
    private final int holdHeapMb;
    private final String slotSharingGroupName;

    private MemoryStressJobOptions(
            MemoryStressMode mode,
            String jobName,
            int parallelism,
            long heartbeatIntervalMs,
            double cpuCores,
            int taskHeapMb,
            int taskOffHeapMb,
            int managedMb,
            int holdHeapMb,
            String slotSharingGroupName) {
        this.mode = mode;
        this.jobName = jobName;
        this.parallelism = parallelism;
        this.heartbeatIntervalMs = heartbeatIntervalMs;
        this.cpuCores = cpuCores;
        this.taskHeapMb = taskHeapMb;
        this.taskOffHeapMb = taskOffHeapMb;
        this.managedMb = managedMb;
        this.holdHeapMb = holdHeapMb;
        this.slotSharingGroupName = slotSharingGroupName;
    }

    public static MemoryStressJobOptions parse(String[] args, MemoryStressMode mode) {
        ParameterTool parameters = ParameterTool.fromArgs(args);
        int holdHeapMb = parameters.getInt("holdHeapMb", defaultHoldHeapMb(parameters, mode));
        int taskHeapMb = parameters.getInt("taskHeapMb", defaultTaskHeapMb(parameters, mode, holdHeapMb));
        int managedMb = parameters.getInt("managedMb", defaultManagedMb(parameters, mode));
        int taskOffHeapMb = parameters.getInt("taskOffHeapMb", 0);
        int parallelism = parameters.getInt("parallelism", DEFAULT_PARALLELISM);
        long heartbeatIntervalMs = parameters.getLong("heartbeatIntervalMs", DEFAULT_HEARTBEAT_INTERVAL_MS);
        double cpuCores = parameters.getDouble("cpuCores", DEFAULT_CPU_CORES);
        String jobName = parameters.get(
                "jobName",
                DEFAULT_JOB_NAME_PREFIX + "-" + mode.name().toLowerCase());
        String slotSharingGroupName = parameters.get(
                "slotSharingGroup",
                DEFAULT_SLOT_SHARING_GROUP + "-" + mode.name().toLowerCase());

        validate(mode, parallelism, heartbeatIntervalMs, cpuCores, taskHeapMb, taskOffHeapMb, managedMb, holdHeapMb);

        return new MemoryStressJobOptions(
                mode,
                jobName,
                parallelism,
                heartbeatIntervalMs,
                cpuCores,
                taskHeapMb,
                taskOffHeapMb,
                managedMb,
                holdHeapMb,
                slotSharingGroupName);
    }

    private static int defaultHoldHeapMb(ParameterTool parameters, MemoryStressMode mode) {
        if (mode == MemoryStressMode.HEAP || mode == MemoryStressMode.COMBINED) {
            return parameters.getInt("heapMb", 64);
        }
        return 0;
    }

    private static int defaultTaskHeapMb(ParameterTool parameters, MemoryStressMode mode, int holdHeapMb) {
        if (parameters.has("heapMb")) {
            return parameters.getInt("heapMb");
        }
        if (mode == MemoryStressMode.MANAGED) {
            return DEFAULT_MANAGED_TASK_HEAP_MB;
        }
        return Math.max(holdHeapMb, 1);
    }

    private static int defaultManagedMb(ParameterTool parameters, MemoryStressMode mode) {
        if (mode == MemoryStressMode.MANAGED || mode == MemoryStressMode.COMBINED) {
            return parameters.getInt("managedMb", 64);
        }
        return parameters.getInt("managedMb", 0);
    }

    private static void validate(
            MemoryStressMode mode,
            int parallelism,
            long heartbeatIntervalMs,
            double cpuCores,
            int taskHeapMb,
            int taskOffHeapMb,
            int managedMb,
            int holdHeapMb) {
        if (parallelism <= 0) {
            throw new IllegalArgumentException("parallelism 必须大于 0");
        }
        if (heartbeatIntervalMs <= 0) {
            throw new IllegalArgumentException("heartbeatIntervalMs 必须大于 0");
        }
        if (cpuCores <= 0) {
            throw new IllegalArgumentException("cpuCores 必须大于 0");
        }
        if (taskHeapMb <= 0) {
            throw new IllegalArgumentException("taskHeapMb 必须大于 0");
        }
        if (taskOffHeapMb < 0) {
            throw new IllegalArgumentException("taskOffHeapMb 不能小于 0");
        }
        if (managedMb < 0) {
            throw new IllegalArgumentException("managedMb 不能小于 0");
        }
        if (holdHeapMb < 0) {
            throw new IllegalArgumentException("holdHeapMb 不能小于 0");
        }
        if (mode == MemoryStressMode.HEAP && holdHeapMb <= 0) {
            throw new IllegalArgumentException("HEAP 模式下 holdHeapMb 必须大于 0");
        }
        if (mode == MemoryStressMode.MANAGED && managedMb <= 0) {
            throw new IllegalArgumentException("MANAGED 模式下 managedMb 必须大于 0");
        }
        if (mode == MemoryStressMode.COMBINED && holdHeapMb <= 0 && managedMb <= 0) {
            throw new IllegalArgumentException("COMBINED 模式下 holdHeapMb 和 managedMb 不能同时为 0");
        }
    }

    public SlotSharingGroup buildSlotSharingGroup() {
        SlotSharingGroup.Builder builder = SlotSharingGroup.newBuilder(slotSharingGroupName)
                .setCpuCores(cpuCores)
                .setTaskHeapMemoryMB(taskHeapMb);
        if (taskOffHeapMb > 0) {
            builder.setTaskOffHeapMemoryMB(taskOffHeapMb);
        }
        if (managedMb > 0) {
            builder.setManagedMemory(MemorySize.ofMebiBytes(managedMb));
        }
        return builder.build();
    }

    public String describe() {
        return "mode=" + mode.name()
                + ", jobName=" + jobName
                + ", parallelism=" + parallelism
                + ", cpuCores=" + cpuCores
                + ", taskHeapMb=" + taskHeapMb
                + ", taskOffHeapMb=" + taskOffHeapMb
                + ", managedMb=" + managedMb
                + ", holdHeapMb=" + holdHeapMb
                + ", heartbeatIntervalMs=" + heartbeatIntervalMs
                + ", slotSharingGroup=" + slotSharingGroupName;
    }

    public MemoryStressMode getMode() {
        return mode;
    }

    public String getJobName() {
        return jobName;
    }

    public int getParallelism() {
        return parallelism;
    }

    public long getHeartbeatIntervalMs() {
        return heartbeatIntervalMs;
    }

    public double getCpuCores() {
        return cpuCores;
    }

    public int getTaskHeapMb() {
        return taskHeapMb;
    }

    public int getTaskOffHeapMb() {
        return taskOffHeapMb;
    }

    public int getManagedMb() {
        return managedMb;
    }

    public int getHoldHeapMb() {
        return holdHeapMb;
    }

    public String getSlotSharingGroupName() {
        return slotSharingGroupName;
    }
}

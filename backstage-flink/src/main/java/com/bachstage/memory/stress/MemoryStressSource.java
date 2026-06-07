package com.bachstage.memory.stress;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 周期性发送心跳并在 JVM Heap 中持有指定内存
 */
public class MemoryStressSource extends RichParallelSourceFunction<String> {
    private static final Logger log = LoggerFactory.getLogger(MemoryStressSource.class);
    private static final int ONE_MB = 1024 * 1024;

    private final MemoryStressJobOptions options;
    private volatile boolean running = true;
    private transient List<byte[]> heapChunks;

    public MemoryStressSource(MemoryStressJobOptions options) {
        this.options = options;
    }

    @Override
    public void open(Configuration parameters) {
        this.heapChunks = allocateHeapChunks(options.getHoldHeapMb());
        log.info("内存压测 Source 初始化完成, subtask={}/{}, holdHeapMb={}, managedMb={}, taskHeapMb={}, taskOffHeapMb={}",
                getRuntimeContext().getIndexOfThisSubtask(),
                getRuntimeContext().getNumberOfParallelSubtasks(),
                options.getHoldHeapMb(),
                options.getManagedMb(),
                options.getTaskHeapMb(),
                options.getTaskOffHeapMb());
    }

    @Override
    public void run(SourceContext<String> ctx) throws Exception {
        while (running) {
            synchronized (ctx.getCheckpointLock()) {
                ctx.collect(buildHeartbeatMessage());
            }
            Thread.sleep(options.getHeartbeatIntervalMs());
        }
    }

    @Override
    public void cancel() {
        running = false;
        releaseHeapChunks();
    }

    @Override
    public void close() {
        releaseHeapChunks();
    }

    private String buildHeartbeatMessage() {
        return "memory-stress-heartbeat"
                + "|mode=" + options.getMode().name()
                + "|subtask=" + getRuntimeContext().getIndexOfThisSubtask()
                + "|parallelism=" + getRuntimeContext().getNumberOfParallelSubtasks()
                + "|holdHeapMb=" + options.getHoldHeapMb()
                + "|managedMb=" + options.getManagedMb()
                + "|taskHeapMb=" + options.getTaskHeapMb();
    }

    private static List<byte[]> allocateHeapChunks(int heapMb) {
        List<byte[]> chunks = new ArrayList<byte[]>();
        for (int i = 0; i < heapMb; i++) {
            chunks.add(new byte[ONE_MB]);
        }
        return chunks;
    }

    private void releaseHeapChunks() {
        if (heapChunks != null) {
            heapChunks.clear();
            heapChunks = null;
        }
    }
}

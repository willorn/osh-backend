package com.bachstage.memory.stress;

/**
 * JVM Heap 压测任务
 */
public class HeapMemoryHoldJob extends AbstractMemoryStressJob {
    public static void main(String[] args) throws Exception {
        new HeapMemoryHoldJob().runJob(args, MemoryStressMode.HEAP);
    }
}

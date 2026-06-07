package com.bachstage.memory.stress;

/**
 * Heap + Managed Memory 组合压测任务
 */
public class CombinedMemoryHoldJob extends AbstractMemoryStressJob {
    public static void main(String[] args) throws Exception {
        new CombinedMemoryHoldJob().runJob(args, MemoryStressMode.COMBINED);
    }
}

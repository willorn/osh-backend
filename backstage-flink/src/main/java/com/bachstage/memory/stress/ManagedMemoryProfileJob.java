package com.bachstage.memory.stress;

/**
 * Managed Memory 资源画像压测任务
 */
public class ManagedMemoryProfileJob extends AbstractMemoryStressJob {
    public static void main(String[] args) throws Exception {
        new ManagedMemoryProfileJob().runJob(args, MemoryStressMode.MANAGED);
    }
}

package com.bachstage.memory.stress;

import org.apache.flink.api.common.operators.SlotSharingGroup;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.DiscardingSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 内存压测任务公共逻辑
 */
public abstract class AbstractMemoryStressJob {
    private static final Logger log = LoggerFactory.getLogger(AbstractMemoryStressJob.class);

    protected void runJob(String[] args, MemoryStressMode mode) throws Exception {
        MemoryStressJobOptions options = MemoryStressJobOptions.parse(args, mode);
        log.info("启动内存压测任务: {}", options.describe());

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(options.getParallelism());
        env.disableOperatorChaining();

        SlotSharingGroup slotSharingGroup = options.buildSlotSharingGroup();
        env.registerSlotSharingGroup(slotSharingGroup);

        DataStream<String> source = env.addSource(new MemoryStressSource(options))
                .name(options.getJobName() + "-source")
                .uid(options.getJobName() + "-source")
                .setParallelism(options.getParallelism())
                .slotSharingGroup(slotSharingGroup);

        source.addSink(new DiscardingSink<String>())
                .name(options.getJobName() + "-discard-sink")
                .uid(options.getJobName() + "-discard-sink")
                .setParallelism(options.getParallelism())
                .slotSharingGroup(slotSharingGroup);

        env.execute(options.getJobName());
    }
}

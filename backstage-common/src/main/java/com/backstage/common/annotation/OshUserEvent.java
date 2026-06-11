package com.backstage.common.annotation;

import com.backstage.common.constant.KafkaConstants;

import java.lang.annotation.*;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 九转苍翎
 * Date: 2026/4/12
 * Time: 12:02
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OshUserEvent {
    //todo: 枚举
    /**
     * 业务模块
     */
    String module() default "";
    /**
     * 操作类型
     */
    String actionType() default "";

    String resourceType() default "";

    /**
     * SpEL expression used to resolve resource type from method args or result.
     * Useful for generic endpoints, for example: #p0.resourceType.
     */
    String resourceTypeExpression() default "";

    /**
     * SpEL expression used to resolve resource id from method args or result.
     * Examples: #args[0].id, #p0.id, #result.data.
     */
    String resourceIdExpression() default "";

    /**
     * SpEL expression used to resolve readable resource name.
     */
    String resourceNameExpression() default "";

    /**
     * SpEL expression used to decide whether the event should be recorded.
     * Empty means always record. Examples: #result.data['paid'] == true
     */
    String recordConditionExpression() default "";
    /**
     * 操作描述
     */
    String description() default "";

    /**
     * Whether anonymous requests should also be recorded.
     */
    boolean recordAnonymous() default false;

    /**
     * Whether only successful executions should be sent.
     */
    boolean successOnly() default false;

    /**
     * Whether this action can be used as a contribution source.
     */
    boolean contribution() default false;

    /**
     * Kafka topic
     */
    String topic() default KafkaConstants.USER_ACTION_TOPIC;
}

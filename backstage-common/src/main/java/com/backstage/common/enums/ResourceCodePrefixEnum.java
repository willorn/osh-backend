package com.backstage.common.enums;

/**
 * 资源编号前缀枚举，用于生成资源编号时标识资源类型。
 * 前缀为两位小写字母。
 */
public enum ResourceCodePrefixEnum {

    COURSE("co", "课程"),
    BOOK("bk", "书籍"),
    TOOL("tl", "工具"),
    COLUMN("cl", "专栏"),
    EXAM("ex", "考试"),
    QUESTION("qa", "问答"),
    OPEN_PROJECT("op", "开源项目"),
    WEBSITE("ws", "实用网站"),
    INFO_GAP("ig", "信息差"),
    ANSWER("aa", "qa_answer"),
    BBS_POST("bp", "bbs_post"),
    BBS_COMMENT("bc", "bbs_comment"),
    COMMENT("cm", "comment"),
    ASSISTANT_FEEDBACK("af", "assistant_feedback"),
    MEMBER_PLAN("mp", "member_plan"),
    ORDER("or", "order"),
    COUPON("cp", "coupon"),
    GROUP("gp", "group"),
    SECKILL("sk", "seckill"),
    INTERNAL_RESOURCE("ir", "internal_resource"),
    ;

    /** 两位小写字母前缀 */
    private final String prefix;

    /** 资源类型描述 */
    private final String desc;

    ResourceCodePrefixEnum(String prefix, String desc) {
        if (prefix == null || prefix.length() != 2) {
            throw new IllegalArgumentException("资源编号前缀必须为两位字母: " + prefix);
        }
        this.prefix = prefix.toLowerCase();
        this.desc = desc;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据前缀查找枚举
     */
    public static ResourceCodePrefixEnum fromPrefix(String prefix) {
        for (ResourceCodePrefixEnum value : values()) {
            if (value.prefix.equals(prefix)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知的资源编号前缀: " + prefix);
    }
}

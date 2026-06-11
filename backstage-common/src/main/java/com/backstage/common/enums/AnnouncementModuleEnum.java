package com.backstage.common.enums;

/**
 * 统一公告表 osh_announcement 的归属模块字典。
 * <p>
 * 该表为站内多业务共用的公告 / 动态存储表，{@code module} 字段标识公告归属的业务模块。
 * 此枚举集中维护所有已知模块的关键字与描述，避免在各处散落字符串字面量。
 *
 * <p>历史说明：早期 tool / info_gap / serial 等模块通过 {@code resource_type} 字段区分，
 * 后续新增模块统一收敛到 {@code module} 字段。此处一并登记，作为模块关键字的唯一字典来源。</p>
 *
 * @author backstage
 */
public enum AnnouncementModuleEnum {

    /**
     * 反馈模块公告
     */
    FEEDBACK("feedback", "反馈公告"),

    /**
     * 秒杀模块（秒杀公告栏 / 动态栏）
     */
    SECKILL("seckill", "秒杀公告"),

    /**
     * 工具模块
     */
    TOOL("tool", "工具公告"),

    /**
     * 信息差模块
     */
    INFO_GAP("info_gap", "信息差公告"),

    /**
     * 开源连载 / 项目模块
     */
    SERIAL("serial", "开源项目公告"),

    /**
     * 实用网站模块
     */
    WEBSITE("website", "实用网站公告"),
//    void SERIAL("serial", "开源项目公告"),

    /**
     * 首页模块公告栏
     */
    HOMEPAGE("homepage", "首页公告");

    /**
     * 模块关键字，落库 / 查询时写入 module 字段
     */
    private final String code;

    /**
     * 模块描述
     */
    private final String desc;

    AnnouncementModuleEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}

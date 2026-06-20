package com.backstage.system.enums.announcement;

/**
 * 通用 WebSocket 事件类型枚举。
 * <p>
 * 面向公告、支付等需要前后端协同识别的实时事件场景，
 * 避免各模块散落硬编码字符串。
 */
public enum AnnouncementWsEventTypeEnum {

    /**
     * 通用公告栏刷新事件。
     */
    ANNOUNCEMENT_REFRESH("ANNOUNCEMENT_REFRESH", "公告栏刷新"),

    /**
     * 支付状态事件示例。
     */
    PAYMENT_STATUS_CHANGED("PAYMENT_STATUS_CHANGED", "支付状态变更");

    private final String code;
    private final String desc;

    AnnouncementWsEventTypeEnum(String code, String desc) {
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

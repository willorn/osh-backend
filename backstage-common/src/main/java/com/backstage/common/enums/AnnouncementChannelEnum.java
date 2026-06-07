package com.backstage.common.enums;

public enum AnnouncementChannelEnum {

    SYSTEM_NOTICE(1, "系统通知"),
    USER_NOTICE(2, "业务公告");

    private final Integer code;
    private final String desc;

    AnnouncementChannelEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据 code 查找枚举值，未匹配则默认返回 SYSTEM_NOTICE
     */
    public static AnnouncementChannelEnum fromCode(Integer code) {
        if (code == null) {
            return SYSTEM_NOTICE;
        }
        for (AnnouncementChannelEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return SYSTEM_NOTICE;
    }
}

package com.backstage.system.enums.homepage;

/**
 * 首页公告资源类型枚举。
 */
public enum HomepageResourceTypeEnum {

    COURSE("course", "课程"),
    BOOK("book", "电子书"),
    EXAM("exam", "考试"),
    QA("qa", "答疑"),
    SECKILL("seckill", "秒杀"),
    GROUP("group", "拼团"),
    INFO_GAP("info_gap", "信息差"),
    OPEN_PROJECT("open_project", "开源项目"),
    WEBSITE("website", "实用网站"),
    FEEDBACK("feedback", "反馈"),
    TOOL("tool", "工具"),
    AUDIT("audit", "审核"),
    ORDER("order", "订单");

    private final String code;
    private final String description;

    HomepageResourceTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static HomepageResourceTypeEnum fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        for (HomepageResourceTypeEnum type : values()) {
            if (type.code.equals(code.trim())) {
                return type;
            }
        }
        return null;
    }

    public static String[] getAllCodes() {
        HomepageResourceTypeEnum[] values = values();
        String[] codes = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            codes[i] = values[i].code;
        }
        return codes;
    }

    public static boolean isValidCode(String code) {
        return fromCode(code) != null;
    }
}

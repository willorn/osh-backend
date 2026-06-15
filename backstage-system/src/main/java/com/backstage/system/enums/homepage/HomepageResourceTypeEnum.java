package com.backstage.system.enums.homepage;

/**
 * 首页公告资源类型枚举
 * <p>
 * 定义首页模块支持的所有资源类型，用于公告数据的分类和推送
 * 
 * @author jayTatum
 */
public enum HomepageResourceTypeEnum {

    /**
     * 课程相关公告
     */
    COURSE("course", "课程", true),

    /**
     * 书籍相关公告
     */
    BOOK("book", "电子书", true),

    /**
     * 考试相关公告
     */
    EXAM("exam", "考试", true),

    /**
     * 秒杀活动相关公告
     */
    FLASHSALE("flashsale", "秒杀", true),

    /**
     * 群组相关公告
     */
    GROUP("group", "拼团", false),

    /**
     * 信息差相关公告
     */
    INFO_GAP("info_gap", "信息差", false),

    /**
     * 实用网站相关公告
     */
    WEBSITE("website", "实用网站", false),

    /**
     * 用户相关公告
     */
    USER("user", "用户", false),

    /**
     * 反馈相关公告
     */
    FEEDBACK("feedback", "反馈", false),

    /**
     * 工具相关公告
     */
    TOOL("tool", "工具", false),

    /**
     * 开源项目相关公告
     */
    OPEN_SOURCE("open_source", "开源项目", false);

    /**
     * 资源类型代码
     */
    private final String code;

    /**
     * 资源类型描述
     */
    private final String description;

    /**
     * 是否为热点资源类型（高频推送）
     */
    private final boolean isHot;

    HomepageResourceTypeEnum(String code, String description, boolean isHot) {
        this.code = code;
        this.description = description;
        this.isHot = isHot;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHot() {
        return isHot;
    }

    /**
     * 根据代码查找枚举值
     * 
     * @param code 资源类型代码
     * @return 对应的枚举值，未找到时返回null
     */
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

    /**
     * 获取所有资源类型代码数组
     * 
     * @return 资源类型代码数组
     */
    public static String[] getAllCodes() {
        HomepageResourceTypeEnum[] values = values();
        String[] codes = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            codes[i] = values[i].code;
        }
        return codes;
    }

    /**
     * 获取热点资源类型代码数组
     * 
     * @return 热点资源类型代码数组
     */
    public static String[] getHotCodes() {
        return java.util.Arrays.stream(values())
                .filter(HomepageResourceTypeEnum::isHot)
                .map(HomepageResourceTypeEnum::getCode)
                .toArray(String[]::new);
    }

    /**
     * 获取普通资源类型代码数组
     * 
     * @return 普通资源类型代码数组
     */
    public static String[] getNormalCodes() {
        return java.util.Arrays.stream(values())
                .filter(type -> !type.isHot)
                .map(HomepageResourceTypeEnum::getCode)
                .toArray(String[]::new);
    }

    /**
     * 验证资源类型代码是否有效
     * 
     * @param code 资源类型代码
     * @return true表示有效，false表示无效
     */
    public static boolean isValidCode(String code) {
        return fromCode(code) != null;
    }
}
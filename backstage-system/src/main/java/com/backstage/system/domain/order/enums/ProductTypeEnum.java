package com.backstage.system.domain.order.enums;

import java.util.Objects;

/**
 * 商品类型枚举
 */
public enum ProductTypeEnum {

    COURSE(1, "course","课程"),

    BOOK(2, "book","书籍"),

    COLUMN(3, "column","专栏"),

    SECKILL(4, "seckill","秒杀"),

    TOOL(5, "tool","工具"),

    GROUP(6, "group", "拼团"),

    MEMBER(7, "member", "会员");
    ;

    private final int code;

    private final String name;
    private final String desc;

    ProductTypeEnum(int code, String name, String desc) {
        this.code = code;
        this.name = name;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据商品类型编码获取枚举。
     *
     * @param code 商品类型编码
     * @return 商品类型枚举，未命中返回 null
     */
    public static ProductTypeEnum fromCode(Integer code) {
        if (Objects.isNull(code)) {
            return null;
        }
        for (ProductTypeEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }

    public static ProductTypeEnum fromName(String name) {
        if (Objects.isNull(name)) {
            return null;
        }
        for (ProductTypeEnum value : values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        return null;
    }
}

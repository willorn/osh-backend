package com.backstage.system.enums.behavior;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public enum BehaviorModuleType {
    USER("用户模块", "用户模块"),
    ADMIN_USER("用户管理", "用户管理"),
    BEHAVIOR("行为数据", "行为数据"),
    COURSE("课程模块", "课程模块"),
    BOOK("电子书模块", "电子书模块"),
    TOOL("工具模块", "工具模块"),
    INFO_GAP("信息差", "信息差"),
    WEBSITE("实用网站", "实用网站"),
    OPEN_PROJECT("开源项目", "开源项目"),
    QUESTION_ANSWER("答疑模块", "答疑模块"),
    BBS("社区帖子", "社区帖子"),
    COMMENT("评论模块", "评论模块"),
    EXAM("考试模块", "考试模块"),
    ORDER("订单模块", "订单模块"),
    PAY("支付模块", "支付模块"),
    MEMBER("会员模块", "会员模块"),
    GROUP("拼团模块", "拼团模块"),
    SECKILL("秒杀模块", "秒杀模块"),
    RESOURCE("内部资源模块", "内部资源模块"),
    AUDIT("资源审核", "资源审核"),
    ASSISTANT("AI助手", "AI助手"),
    FEEDBACK("需求反馈", "需求反馈"),
    HOMEPAGE("首页模块", "首页模块"),
    SITE("站点模块", "站点模块"),
    UPLOAD("上传模块", "上传模块"),
    SEARCH("搜索模块", "搜索模块"),
    COUPON("优惠券模块", "优惠券模块"),
    FRIEND_LINK("友链模块", "友链模块");

    private final String code;
    private final String label;

    BehaviorModuleType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static List<Map<String, String>> options() {
        List<Map<String, String>> rows = new ArrayList<>();
        for (BehaviorModuleType value : values()) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("code", value.code);
            row.put("label", value.label);
            rows.add(row);
        }
        return rows;
    }
}

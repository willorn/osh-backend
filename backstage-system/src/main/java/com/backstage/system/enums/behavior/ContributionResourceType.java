package com.backstage.system.enums.behavior;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public enum ContributionResourceType {
    COURSE("course", "课程", true, true),
    BOOK("book", "电子书", true, true),
    TOOL("tool", "工具", true, true),
    INFO_GAP("info_gap", "信息差", false, true),
    WEBSITE("website", "实用网站", false, true),
    OPEN_PROJECT("open_project", "开源项目", false, true),
    QA_QUESTION("qa_question", "答疑问题", false, false),
    QA_ANSWER("qa_answer", "答疑回答", false, false),
    QA_TAG("qa_tag", "答疑标签", false, false),
    BBS_POST("bbs_post", "社区帖子", false, false),
    BBS_COMMENT("bbs_comment", "社区评论", false, false),
    EXAM("exam", "考试", true, false),
    EXAM_QUESTION("exam_question", "试题", false, false),
    COMMENT("comment", "评论", false, false),
    ASSISTANT_FEEDBACK("assistant_feedback", "需求反馈", false, false),
    MEMBER_PLAN("member_plan", "会员套餐", true, false),
    ORDER("order", "订单", true, false),
    COUPON("coupon", "优惠券", false, false),
    GROUP("group", "拼团", true, false),
    SECKILL("seckill", "秒杀", true, false),
    INTERNAL_RESOURCE("internal_resource", "内部资源", false, false);

    private final String code;
    private final String label;
    private final boolean paidCapable;
    private final boolean contributionTracked;

    ContributionResourceType(String code, String label, boolean paidCapable, boolean contributionTracked) {
        this.code = code;
        this.label = label;
        this.paidCapable = paidCapable;
        this.contributionTracked = contributionTracked;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public boolean isPaidCapable() {
        return paidCapable;
    }

    public boolean isContributionTracked() {
        return contributionTracked;
    }

    public static boolean supported(String code) {
        return fromCode(code) != null;
    }

    public static boolean contributionTracked(String code) {
        ContributionResourceType type = fromCode(code);
        return type != null && type.contributionTracked;
    }

    public static ContributionResourceType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ContributionResourceType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static List<Map<String, Object>> options() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ContributionResourceType value : values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", value.code);
            row.put("label", value.label);
            row.put("paidCapable", value.paidCapable);
            row.put("contributionTracked", value.contributionTracked);
            rows.add(row);
        }
        return rows;
    }
}

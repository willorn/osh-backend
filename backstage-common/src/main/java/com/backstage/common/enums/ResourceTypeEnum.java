package com.backstage.common.enums;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 九转苍翎
 * Date: 2026/4/23
 * Time: 18:26
 */
public enum ResourceTypeEnum {
    COURSE("course", "osh_course", "osh_course_search_read"),
    QA_QUESTION("qa_question", "osh_question_answer_question", ""),
    QA_ANSWER("qa_answer", "osh_question_answer_answer", ""),
    QA_TAG("qa_tag", "tag", ""),
    BOOK("book", "osh_book", ""),
    TOOL("tool", "osh_tool", "osh_tool_search"),
    WEBSITE("website", "osh_practical_website", ""),
    OPEN_PROJECT("open_project", "osh_open_project", ""),
    INFO_GAP("info_gap", "osh_info_gap", ""),
    BBS_POST("bbs_post", "osh_bbs_post", ""),
    BBS_COMMENT("bbs_comment", "osh_bbs_comment", ""),
    EXAM("exam", "osh_examination", ""),
    EXAM_QUESTION("exam_question", "osh_exam_question", ""),
    COMMENT("comment", "osh_comment", ""),
    ASSISTANT_FEEDBACK("assistant_feedback", "assistant_feedback", ""),
    MEMBER_PLAN("member_plan", "osh_member_plan", ""),
    ORDER("order", "osh_order", ""),
    COUPON("coupon", "osh_card", ""),
    GROUP("group", "osh_group", ""),
    SECKILL("seckill", "osh_seckill_activity", ""),
    INTERNAL_RESOURCE("internal_resource", "osh_resource", ""),
    ;
    private final String type;
    private final String mysqlTableName;
    private final String esIndexName;


    ResourceTypeEnum(String type, String mysqlTableName, String esIndexName) {
        this.type = type;
        this.mysqlTableName = mysqlTableName;
        this.esIndexName = esIndexName;
    }

    public String getType() {
        return type;
    }

    public String getMysqlTableName() {
        return mysqlTableName;
    }

    public String getEsIndexName() {
        return esIndexName;
    }

    public static ResourceTypeEnum fromTypeCode(String type) {
        for (ResourceTypeEnum curType : values()) {
            if (curType.getType().equals(type)) {
                return curType;
            }
        }
        throw new IllegalArgumentException("未知的资源类型: " + type);
    }
}

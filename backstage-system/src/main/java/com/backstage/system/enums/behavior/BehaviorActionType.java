package com.backstage.system.enums.behavior;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public enum BehaviorActionType {
    CREATE("新增", "新增"),
    UPDATE("修改", "修改"),
    EDIT("编辑", "编辑"),
    DELETE("删除", "删除"),
    QUERY("查询", "查询"),
    SEARCH("搜索", "搜索"),
    VIEW("浏览", "浏览"),
    CLICK("点击", "点击"),
    SUBMIT("提交", "提交"),
    AUDIT("审核", "审核"),
    APPROVE("通过", "通过"),
    REJECT("驳回", "驳回"),
    COLLECT("收藏", "收藏"),
    UNCOLLECT("取消收藏", "取消收藏"),
    FAVORITE("关注", "关注"),
    UNFAVORITE("取消关注", "取消关注"),
    LIKE("点赞", "点赞"),
    UNLIKE("取消点赞", "取消点赞"),
    VOTE("评价", "评价"),
    PURCHASE("购买", "购买"),
    PAY("支付", "支付"),
    REFUND("退款", "退款"),
    USE("使用", "使用"),
    CONSUMPTION("消费", "消费"),
    CONSUME("消耗", "消耗"),
    UPLOAD("上传", "上传"),
    DOWNLOAD("下载", "下载"),
    LOGIN("登录", "登录"),
    LOGOUT("登出", "登出"),
    REGISTER("注册", "注册"),
    SEND_CODE("发送验证码", "发送验证码"),
    CHANGE_EMAIL("修改邮箱", "修改邮箱"),
    RESET_PASSWORD("找回密码", "找回密码"),
    UPDATE_PROFILE("修改资料", "修改资料"),
    CHANGE_PASSWORD("修改密码", "修改密码"),
    CANCEL_ACCOUNT("注销用户", "注销用户"),
    UPDATE_ASSET("更新资产", "更新资产"),
    VIOLATION_RECORD("违规记录", "违规记录"),
    RECOMMEND("推荐", "推荐"),
    LEARN("学习", "学习"),
    ANSWER("回答", "回答"),
    COMMENT("评论", "评论"),
    OTHER("其他", "其他");

    private final String code;
    private final String label;

    BehaviorActionType(String code, String label) {
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
        for (BehaviorActionType value : values()) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("code", value.code);
            row.put("label", value.label);
            rows.add(row);
        }
        return rows;
    }
}

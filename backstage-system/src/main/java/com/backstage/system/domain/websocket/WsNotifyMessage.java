package com.backstage.system.domain.websocket;

import com.backstage.common.core.domain.entity.OSHBaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

/**
 * WebSocket 通用推送消息体，同时作为持久化实体
 * 对应表：osh_ws_notification
 */
@TableName("osh_ws_notification")
public class WsNotifyMessage extends OSHBaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 接收消息的用户 ID */
    private Long targetUserId;

    /** 消息类型，由业务方自定义（如 QA_NEW_ANSWER） */
    private String type;

    /** 通知标题 */
    private String title;

    /** 通知内容摘要（可选） */
    private String content;

    /** 点击跳转的前端路由（可选，为空则不跳转） */
    private String jumpUrl;

    /** 业务 ID（可选，供前端跳转或二次请求使用） */
    private String bizId;

    /** 是否需要认证/登录才能接收此消息（默认false） */
    private boolean requireAuth = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getJumpUrl() { return jumpUrl; }
    public void setJumpUrl(String jumpUrl) { this.jumpUrl = jumpUrl; }

    public String getBizId() { return bizId; }
    public void setBizId(String bizId) { this.bizId = bizId; }

    public boolean isRequireAuth() { return requireAuth; }
    public void setRequireAuth(boolean requireAuth) { this.requireAuth = requireAuth; }
}

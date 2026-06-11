package com.backstage.common.core.domain;

import com.backstage.common.core.domain.entity.OSHBaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 九转苍翎
 * Date: 2026/4/12
 * Time: 16:45
 */
@TableName(value = "osh_user_action_event")
public class OshUserEvent extends OSHBaseEntity {
    /**
     * 主键ID（建议添加，便于分页和删除）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 用户id
     */
    @TableField("user_id")
    private Long userId;
    /**
     * 用户名
     */
    @TableField("username")
    private String username;
    /**
     * 角色
     */
    @TableField("role")
    private String role;
    @TableField("role_level")
    private Integer roleLevel;
    /**
     * 模块名称
     */
    @TableField("module")
    private String module;
    /**
     * 方法名称
     */
    @TableField("method_name")
    private String methodName;
    /**
     * 操作类型
     */
    @TableField("action_type")
    private String actionType;
    /**
     * 资源id
     */
    @TableField("resource_id")
    private String ResourceId;
    /**
     * 资源类型
     */
    @TableField("resource_type")
    private String resourceType;
    @TableField("resource_name")
    private String resourceName;
    /**
     * 描述
     */
    @TableField("description")
    private String description;
    /**
     * 状态：0-失败，1-成功
     */
    @TableField("status")
    private String status;
    /**
     * 异常信息
     */
    @TableField("exception")
    private String exception;
    @TableField("request_uri")
    private String requestUri;
    @TableField("request_method")
    private String requestMethod;
    @TableField("ip")
    private String ip;
    @TableField("user_agent")
    private String userAgent;
    @TableField("duration_ms")
    private Long durationMs;
    @TableField("result_code")
    private String resultCode;
    @TableField("trace_id")
    private String traceId;
    @TableField("contribution")
    private Integer contribution;
    /**
     * 当前积分
     */
    @TableField("current_point")
    private Long currentPoint;

    /**
     * 发生时间
     */
    @TableField("happen_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSSSSSSSS")
    private LocalDateTime happenTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getRoleLevel() {
        return roleLevel;
    }

    public void setRoleLevel(Integer roleLevel) {
        this.roleLevel = roleLevel;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getResourceId() {
        return ResourceId;
    }

    public void setResourceId(String resourceId) {
        ResourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Integer getContribution() {
        return contribution;
    }

    public void setContribution(Integer contribution) {
        this.contribution = contribution;
    }

    public Long getCurrentPoint() {
        return currentPoint;
    }

    public void setCurrentPoint(Long currentPoint) {
        this.currentPoint = currentPoint;
    }

    public LocalDateTime getHappenTime() {
        return happenTime;
    }

    public void setHappenTime(LocalDateTime happenTime) {
        this.happenTime = happenTime;
    }

    @Override
    public String toString() {
        return "OshUserEvent{" +
                "id=" + id +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", roleLevel=" + roleLevel +
                ", module='" + module + '\'' +
                ", methodName='" + methodName + '\'' +
                ", actionType='" + actionType + '\'' +
                ", resourceId='" + ResourceId + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", resourceName='" + resourceName + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", exception='" + exception + '\'' +
                ", requestUri='" + requestUri + '\'' +
                ", requestMethod='" + requestMethod + '\'' +
                ", ip='" + ip + '\'' +
                ", durationMs=" + durationMs +
                ", resultCode='" + resultCode + '\'' +
                ", traceId='" + traceId + '\'' +
                ", contribution=" + contribution +
                ", currentPoint=" + currentPoint +
                ", happenTime='" + happenTime + '\'' +
                '}';
    }

    public OshUserEvent() {
    }

    public OshUserEvent(Long id, Long userId, String username, String role, String module,
                        String methodName, String actionType, String ResourceId, String resourceType,
                        String description, String status, String exception,
                        Long currentPoint, LocalDateTime happenTime) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.module = module;
        this.methodName = methodName;
        this.actionType = actionType;
        this.ResourceId = ResourceId;
        this.resourceType = resourceType;
        this.description = description;
        this.status = status;
        this.exception = exception;
        this.currentPoint = currentPoint;
        this.happenTime = happenTime;
    }

    public OshUserEvent(Long id, Long userId, String username, String role, Integer roleLevel, String module,
                        String methodName, String actionType, String resourceId, String resourceType,
                        String resourceName, String description, String status, String exception,
                        Long currentPoint, LocalDateTime happenTime) {
        this(id, userId, username, role, module, methodName, actionType, resourceId, resourceType,
                description, status, exception, currentPoint, happenTime);
        this.roleLevel = roleLevel;
        this.resourceName = resourceName;
    }
}

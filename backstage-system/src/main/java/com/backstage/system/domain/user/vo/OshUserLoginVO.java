package com.backstage.system.domain.user.vo;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * OshUser: 九转苍翎
 * Date: 2026/3/12
 * Time: 20:16
 */
public class OshUserLoginVO {
    private Long userId;

    private String username;

    private String token;

    private Map<String, String> asset;

    private Map<String, String> role;

    private Map<String,List<String>> permissionList;

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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Map<String, String> getAsset() {
        return asset;
    }

    public void setAsset(Map<String, String> asset) {
        this.asset = asset;
    }

    public Map<String, String> getRole() {
        return role;
    }

    public void setRole(Map<String, String> role) {
        this.role = role;
    }

    public Map<String, List<String>> getPermissionList() {
        return permissionList;
    }

    public void setPermissionList(Map<String, List<String>> permissionList) {
        this.permissionList = permissionList;
    }

    @Override
    public String toString() {
        return "OshUserLoginVO{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", token='" + token + '\'' +
                ", asset=" + asset +
                ", role=" + role +
                ", permissionList=" + permissionList +
                '}';
    }
}

package com.backstage.system.domain.user.vo;

import java.util.List;
import java.util.Map;

public class OshUserCardVO {
    private Long id;
    private String username;
    private String avatar;
    private String githubAccount;
    private String wechatName;
    private String sex;
    private String introduction;
    private List<Map<String, Object>> roles;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getGithubAccount() { return githubAccount; }
    public void setGithubAccount(String githubAccount) { this.githubAccount = githubAccount; }
    public String getWechatName() { return wechatName; }
    public void setWechatName(String wechatName) { this.wechatName = wechatName; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public String getIntroduction() { return introduction; }
    public void setIntroduction(String introduction) { this.introduction = introduction; }
    public List<Map<String, Object>> getRoles() { return roles; }
    public void setRoles(List<Map<String, Object>> roles) { this.roles = roles; }
}

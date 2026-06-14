package com.backstage.system.domain.user.dto;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * OshUser: 九转苍翎
 * Date: 2026/3/7
 * Time: 19:42
 */
public class UserUpdateInfoDTO {
    private String username;
    private String sex;
    private String introduction;
    private String githubAccount;
    private String wechatName;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public String getGithubAccount() {
        return githubAccount;
    }

    public void setGithubAccount(String githubAccount) {
        this.githubAccount = githubAccount;
    }

    public String getWechatName() {
        return wechatName;
    }

    public void setWechatName(String wechatName) {
        this.wechatName = wechatName;
    }

    @Override
    public String toString() {
        return "UserUpdateInfoDTO{" +
                "username='" + username + '\'' +
                ", sex='" + sex + '\'' +
                ", introduction='" + introduction + '\'' +
                ", githubAccount='" + githubAccount + '\'' +
                ", wechatName='" + wechatName + '\'' +
                '}';
    }
}

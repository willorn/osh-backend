package com.backstage.system.domain.openproject.dto;

public class OpenProjectContributorDTO {
    private String githubAccount;
    private String wechatName;
    private String contributorType;
    private Integer contributions;
    private String avatarUrl;
    private String profileUrl;
    private Integer sortOrder;

    public String getGithubAccount() { return githubAccount; }
    public void setGithubAccount(String githubAccount) { this.githubAccount = githubAccount; }
    public String getWechatName() { return wechatName; }
    public void setWechatName(String wechatName) { this.wechatName = wechatName; }
    public String getContributorType() { return contributorType; }
    public void setContributorType(String contributorType) { this.contributorType = contributorType; }
    public Integer getContributions() { return contributions; }
    public void setContributions(Integer contributions) { this.contributions = contributions; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getProfileUrl() { return profileUrl; }
    public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}

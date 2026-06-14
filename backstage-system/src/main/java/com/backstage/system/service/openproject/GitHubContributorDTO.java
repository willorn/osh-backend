package com.backstage.system.service.openproject;

public class GitHubContributorDTO {
    private String githubAccount;
    private Integer contributions;
    private String avatarUrl;
    private String profileUrl;
    private String contributorType;

    public String getGithubAccount() { return githubAccount; }
    public void setGithubAccount(String githubAccount) { this.githubAccount = githubAccount; }
    public Integer getContributions() { return contributions; }
    public void setContributions(Integer contributions) { this.contributions = contributions; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getProfileUrl() { return profileUrl; }
    public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }
    public String getContributorType() { return contributorType; }
    public void setContributorType(String contributorType) { this.contributorType = contributorType; }
}

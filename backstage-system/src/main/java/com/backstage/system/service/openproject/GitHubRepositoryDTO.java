package com.backstage.system.service.openproject;

import java.time.LocalDateTime;

public class GitHubRepositoryDTO {
    private Long githubRepoId;
    private String name;
    private String fullName;
    private String htmlUrl;
    private String description;
    private String ownerLogin;
    private String ownerAvatarUrl;
    private String ownerHtmlUrl;
    private Integer stargazersCount;
    private Integer forksCount;
    private Boolean archived;
    private Boolean privateRepo;
    private String defaultBranch;
    private String language;
    private String licenseName;
    private String homepage;
    private LocalDateTime pushedAt;

    public Long getGithubRepoId() { return githubRepoId; }
    public void setGithubRepoId(Long githubRepoId) { this.githubRepoId = githubRepoId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOwnerLogin() { return ownerLogin; }
    public void setOwnerLogin(String ownerLogin) { this.ownerLogin = ownerLogin; }
    public String getOwnerAvatarUrl() { return ownerAvatarUrl; }
    public void setOwnerAvatarUrl(String ownerAvatarUrl) { this.ownerAvatarUrl = ownerAvatarUrl; }
    public String getOwnerHtmlUrl() { return ownerHtmlUrl; }
    public void setOwnerHtmlUrl(String ownerHtmlUrl) { this.ownerHtmlUrl = ownerHtmlUrl; }
    public Integer getStargazersCount() { return stargazersCount; }
    public void setStargazersCount(Integer stargazersCount) { this.stargazersCount = stargazersCount; }
    public Integer getForksCount() { return forksCount; }
    public void setForksCount(Integer forksCount) { this.forksCount = forksCount; }
    public Boolean getArchived() { return archived; }
    public void setArchived(Boolean archived) { this.archived = archived; }
    public Boolean getPrivateRepo() { return privateRepo; }
    public void setPrivateRepo(Boolean privateRepo) { this.privateRepo = privateRepo; }
    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getLicenseName() { return licenseName; }
    public void setLicenseName(String licenseName) { this.licenseName = licenseName; }
    public String getHomepage() { return homepage; }
    public void setHomepage(String homepage) { this.homepage = homepage; }
    public LocalDateTime getPushedAt() { return pushedAt; }
    public void setPushedAt(LocalDateTime pushedAt) { this.pushedAt = pushedAt; }
}

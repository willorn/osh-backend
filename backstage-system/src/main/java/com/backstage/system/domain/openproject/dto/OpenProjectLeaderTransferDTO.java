package com.backstage.system.domain.openproject.dto;

import com.backstage.common.annotation.OshResourceId;

public class OpenProjectLeaderTransferDTO {
    @OshResourceId
    private Long projectId;
    private Long contributorId;
    private String githubAccount;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getContributorId() { return contributorId; }
    public void setContributorId(Long contributorId) { this.contributorId = contributorId; }
    public String getGithubAccount() { return githubAccount; }
    public void setGithubAccount(String githubAccount) { this.githubAccount = githubAccount; }
}

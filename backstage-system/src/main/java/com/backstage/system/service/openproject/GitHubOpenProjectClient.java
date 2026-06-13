package com.backstage.system.service.openproject;

import java.util.List;

public interface GitHubOpenProjectClient {
    List<GitHubRepositoryDTO> listPublicRepositories(String owner, String sourceType, String token);

    List<GitHubContributorDTO> listContributors(String owner, String repo, String token);
}

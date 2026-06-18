package com.backstage.system.service.openproject.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.backstage.system.service.openproject.GitHubContributorDTO;
import com.backstage.system.service.openproject.GitHubOpenProjectClient;
import com.backstage.system.service.openproject.GitHubRepositoryDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class GitHubOpenProjectClientImpl implements GitHubOpenProjectClient {

    private static final String API = "https://api.github.com";

    @Override
    public List<GitHubRepositoryDTO> listPublicRepositories(String owner, String sourceType, String token) {
        List<GitHubRepositoryDTO> result = new ArrayList<>();
        int page = 1;
        String ownerTypePath = "org".equalsIgnoreCase(sourceType) ? "/orgs/" : "/users/";
        while (page <= 10) {
            String body = httpGet(API + ownerTypePath + owner + "/repos?type=public&sort=updated&per_page=100&page=" + page, token);
            JSONArray array = JSON.parseArray(body);
            if (array == null || array.isEmpty()) {
                break;
            }
            for (int i = 0; i < array.size(); i++) {
                GitHubRepositoryDTO repo = toRepo(array.getJSONObject(i));
                if (!Boolean.TRUE.equals(repo.getPrivateRepo())) {
                    result.add(repo);
                }
            }
            if (array.size() < 100) {
                break;
            }
            page++;
        }
        return result;
    }

    @Override
    public List<GitHubContributorDTO> listContributors(String owner, String repo, String token) {
        List<GitHubContributorDTO> result = new ArrayList<>();
        int page = 1;
        while (page <= 10) {
            String body = httpGet(API + "/repos/" + owner + "/" + repo + "/contributors?per_page=100&page=" + page, token);
            JSONArray array = JSON.parseArray(body);
            if (array == null || array.isEmpty()) {
                break;
            }
            for (int i = 0; i < array.size(); i++) {
                JSONObject item = array.getJSONObject(i);
                GitHubContributorDTO dto = new GitHubContributorDTO();
                dto.setGithubAccount(item.getString("login"));
                dto.setContributions(item.getIntValue("contributions"));
                dto.setAvatarUrl(item.getString("avatar_url"));
                dto.setProfileUrl(item.getString("html_url"));
                dto.setContributorType("contributor");
                result.add(dto);
            }
            if (array.size() < 100) {
                break;
            }
            page++;
        }
        return result;
    }

    private GitHubRepositoryDTO toRepo(JSONObject item) {
        GitHubRepositoryDTO dto = new GitHubRepositoryDTO();
        dto.setGithubRepoId(item.getLong("id"));
        dto.setName(item.getString("name"));
        dto.setFullName(item.getString("full_name"));
        dto.setHtmlUrl(item.getString("html_url"));
        dto.setDescription(item.getString("description"));
        dto.setStargazersCount(item.getIntValue("stargazers_count"));
        dto.setForksCount(item.getIntValue("forks_count"));
        dto.setArchived(item.getBoolean("archived"));
        dto.setPrivateRepo(item.getBoolean("private"));
        dto.setDefaultBranch(item.getString("default_branch"));
        dto.setLanguage(item.getString("language"));
        dto.setHomepage(item.getString("homepage"));
        dto.setPushedAt(parseGitHubTime(item.getString("pushed_at")));
        JSONObject owner = item.getJSONObject("owner");
        if (owner != null) {
            dto.setOwnerLogin(owner.getString("login"));
            dto.setOwnerAvatarUrl(owner.getString("avatar_url"));
            dto.setOwnerHtmlUrl(owner.getString("html_url"));
        }
        JSONObject license = item.getJSONObject("license");
        if (license != null) {
            dto.setLicenseName(license.getString("name"));
        }
        return dto;
    }

    private LocalDateTime parseGitHubTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return LocalDateTime.parse(value.replace("Z", ""), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }

    private String httpGet(String urlStr, String token) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("User-Agent", "osh-backend");
            if (StringUtils.hasText(token)) {
                conn.setRequestProperty("Authorization", "token " + token);
            }
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("GitHub API returned " + code + " for " + urlStr);
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}

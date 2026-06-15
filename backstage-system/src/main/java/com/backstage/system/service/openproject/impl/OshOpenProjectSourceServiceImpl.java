package com.backstage.system.service.openproject.impl;

import com.backstage.system.domain.openproject.OshOpenProject;
import com.backstage.system.domain.openproject.OshOpenProjectContributor;
import com.backstage.system.domain.openproject.OshOpenProjectSource;
import com.backstage.system.domain.openproject.dto.OpenProjectSourceDTO;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.mapper.openproject.OshOpenProjectContributorMapper;
import com.backstage.system.mapper.openproject.OshOpenProjectMapper;
import com.backstage.system.mapper.openproject.OshOpenProjectSourceMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.openproject.GitHubContributorDTO;
import com.backstage.system.service.openproject.GitHubOpenProjectClient;
import com.backstage.system.service.openproject.GitHubRepositoryDTO;
import com.backstage.system.service.openproject.IOshOpenProjectRankService;
import com.backstage.system.service.openproject.IOshOpenProjectSourceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class OshOpenProjectSourceServiceImpl implements IOshOpenProjectSourceService {

    private static final int MAX_URL_LENGTH = 500;
    private static final int MAX_TEXT_LENGTH = 500;
    private static final String MASKED_TOKEN = "******";

    @Autowired
    private OshOpenProjectSourceMapper sourceMapper;

    @Autowired
    private OshOpenProjectMapper projectMapper;

    @Autowired
    private OshOpenProjectContributorMapper contributorMapper;

    @Autowired
    private OshUserMapper userMapper;

    @Autowired
    private GitHubOpenProjectClient githubClient;

    @Autowired
    private IOshOpenProjectRankService rankService;

    @Override
    public List<OshOpenProjectSource> listSources() {
        List<OshOpenProjectSource> sources = sourceMapper.selectList(new LambdaQueryWrapper<OshOpenProjectSource>()
                .eq(OshOpenProjectSource::getDeleteFlag, (byte) 0)
                .orderByDesc(OshOpenProjectSource::getCreateTime));
        for (OshOpenProjectSource source : sources) {
            if (StringUtils.hasText(source.getAccessToken())) {
                source.setAccessToken(MASKED_TOKEN);
            }
        }
        return sources;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OshOpenProjectSource saveSource(OpenProjectSourceDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("项目源不能为空");
        }
        String owner = normalizeOwner(dto.getGithubOwner(), dto.getGithubUrl());
        if (!StringUtils.hasText(owner)) {
            throw new IllegalArgumentException("GitHub账号/组织不能为空");
        }

        OshOpenProjectSource source = dto.getId() == null ? null : sourceMapper.selectById(dto.getId());
        if (source == null) {
            source = new OshOpenProjectSource();
            source.setDeleted(false);
            source.setSyncStatus(0);
            source.setRepoCount(0);
        }
        source.setGithubOwner(owner);
        source.setGithubUrl("https://github.com/" + owner);
        source.setSourceName(trimToMax(StringUtils.hasText(dto.getSourceName()) ? dto.getSourceName() : owner, 100));
        source.setSourceType(normalizeSourceType(dto.getSourceType()));
        if (source.getId() == null || dto.getAccessToken() != null && !MASKED_TOKEN.equals(dto.getAccessToken())) {
            source.setAccessToken(trimToMax(dto.getAccessToken(), 500));
        }
        source.setEnabled(dto.getEnabled() == null ? 1 : (dto.getEnabled() == 1 ? 1 : 0));
        source.setRemark(trimToMax(dto.getRemark(), 500));

        if (source.getId() == null) {
            ensureOwnerNotExists(owner, null);
            sourceMapper.insert(source);
        } else {
            ensureOwnerNotExists(owner, source.getId());
            sourceMapper.updateById(source);
        }
        return source;
    }

    @Override
    public void deleteSource(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("项目源ID不能为空");
        }
        sourceMapper.update(null, new LambdaUpdateWrapper<OshOpenProjectSource>()
                .eq(OshOpenProjectSource::getId, id)
                .set(OshOpenProjectSource::getDeleteFlag, (byte) 1)
                .set(OshOpenProjectSource::getEnabled, 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncSource(Long sourceId) {
        if (sourceId == null) {
            throw new IllegalArgumentException("项目源ID不能为空");
        }
        OshOpenProjectSource source = sourceMapper.selectById(sourceId);
        if (source == null || source.getDeleteFlag() != null && source.getDeleteFlag() == 1) {
            throw new IllegalArgumentException("项目源不存在");
        }
        return syncOneSource(source);
    }

    @Override
    public int syncAllEnabledSources() {
        List<OshOpenProjectSource> sources = sourceMapper.selectList(new LambdaQueryWrapper<OshOpenProjectSource>()
                .eq(OshOpenProjectSource::getEnabled, 1)
                .eq(OshOpenProjectSource::getDeleteFlag, (byte) 0));
        int total = 0;
        for (OshOpenProjectSource source : sources) {
            total += syncOneSource(source);
        }
        return total;
    }

    private int syncOneSource(OshOpenProjectSource source) {
        int count = 0;
        try {
            List<GitHubRepositoryDTO> repos = githubClient.listPublicRepositories(source.getGithubOwner(), source.getSourceType(), source.getAccessToken());
            for (GitHubRepositoryDTO repo : repos) {
                OshOpenProject project = upsertProject(source, repo);
                syncContributors(project, source.getAccessToken());
                count++;
            }
            rankService.saveTodaySnapshot();
            updateSourceSyncResult(source.getId(), 1, count, "同步成功");
            return count;
        } catch (Exception e) {
            updateSourceSyncResult(source.getId(), 2, count, trimToMax(e.getMessage(), MAX_TEXT_LENGTH));
            throw e;
        }
    }

    private OshOpenProject upsertProject(OshOpenProjectSource source, GitHubRepositoryDTO repo) {
        OshOpenProject project = null;
        if (repo.getGithubRepoId() != null) {
            project = projectMapper.selectOne(new LambdaQueryWrapper<OshOpenProject>()
                    .eq(OshOpenProject::getGithubRepoId, repo.getGithubRepoId())
                    .last("limit 1"));
        }
        if (project == null && StringUtils.hasText(repo.getHtmlUrl())) {
            project = projectMapper.selectOne(new LambdaQueryWrapper<OshOpenProject>()
                    .eq(OshOpenProject::getProjectUrl, repo.getHtmlUrl())
                    .last("limit 1"));
        }
        boolean insert = project == null;
        if (insert) {
            project = new OshOpenProject();
            project.setStatus(1);
            project.setClickCount(0);
            project.setDeleted(false);
        }
        project.setSourceId(source.getId());
        project.setGithubRepoId(repo.getGithubRepoId());
        project.setGithubOwner(repo.getOwnerLogin());
        project.setGithubRepoName(repo.getName());
        project.setProjectName(trimToMax(repo.getName(), 100));
        project.setProjectDesc(trimToMax(repo.getDescription(), MAX_TEXT_LENGTH));
        project.setProjectUrl(trimToMax(repo.getHtmlUrl(), MAX_URL_LENGTH));
        project.setAuthorName(trimToMax(repo.getOwnerLogin(), 100));
        project.setProjectCover(trimToMax(repo.getOwnerAvatarUrl(), MAX_URL_LENGTH));
        project.setStarCount(repo.getStargazersCount() == null ? 0 : repo.getStargazersCount());
        project.setForkCount(repo.getForksCount() == null ? 0 : repo.getForksCount());
        project.setLastCommitTime(repo.getPushedAt());
        project.setIsArchived((byte) (Boolean.TRUE.equals(repo.getArchived()) ? 1 : 0));
        project.setLastSyncTime(LocalDateTime.now());
        project.setDefaultBranch(trimToMax(repo.getDefaultBranch(), 100));
        project.setLanguage(trimToMax(repo.getLanguage(), 100));
        project.setLicenseName(trimToMax(repo.getLicenseName(), 100));
        project.setHomepage(trimToMax(repo.getHomepage(), MAX_URL_LENGTH));
        if (insert) {
            projectMapper.insert(project);
        } else {
            projectMapper.updateById(project);
        }
        return project;
    }

    private void syncContributors(OshOpenProject project, String token) {
        try {
            List<GitHubContributorDTO> contributors = githubClient.listContributors(project.getGithubOwner(), project.getGithubRepoName(), token);
            int sort = 0;
            for (GitHubContributorDTO item : contributors) {
                if (!StringUtils.hasText(item.getGithubAccount())) {
                    continue;
                }
                OshOpenProjectContributor contributor = contributorMapper.selectOne(
                        new LambdaQueryWrapper<OshOpenProjectContributor>()
                                .eq(OshOpenProjectContributor::getProjectId, project.getId())
                                .eq(OshOpenProjectContributor::getGithubAccount, item.getGithubAccount())
                                .last("limit 1"));
                if (contributor == null) {
                    contributor = new OshOpenProjectContributor();
                    contributor.setProjectId(project.getId());
                    contributor.setGithubAccount(item.getGithubAccount());
                    contributor.setDeleted(false);
                    contributor.setEditable(1);
                }
                contributor.setContributorType(item.getContributorType());
                contributor.setContributions(item.getContributions());
                contributor.setAvatarUrl(trimToMax(item.getAvatarUrl(), MAX_URL_LENGTH));
                contributor.setProfileUrl(trimToMax(item.getProfileUrl(), MAX_URL_LENGTH));
                contributor.setSource("github");
                contributor.setSortOrder(sort++);
                contributor.setWechatName(resolveWechatName(item.getGithubAccount()));
                if (contributor.getId() == null) {
                    contributorMapper.insert(contributor);
                } else {
                    contributorMapper.updateById(contributor);
                }
            }
        } catch (Exception ignored) {
            // Repository sync should not fail just because contributor sync is unavailable/rate-limited.
        }
    }

    private String resolveWechatName(String githubAccount) {
        OshUser user = userMapper.selectOne(new LambdaQueryWrapper<OshUser>()
                .eq(OshUser::getGithubAccount, githubAccount)
                .eq(OshUser::getDeleteFlag, (byte) 0)
                .last("limit 1"));
        return user == null ? null : user.getWechatName();
    }

    private void updateSourceSyncResult(Long sourceId, int status, int repoCount, String message) {
        sourceMapper.update(null, new LambdaUpdateWrapper<OshOpenProjectSource>()
                .eq(OshOpenProjectSource::getId, sourceId)
                .set(OshOpenProjectSource::getSyncStatus, status)
                .set(OshOpenProjectSource::getRepoCount, repoCount)
                .set(OshOpenProjectSource::getLastSyncTime, LocalDateTime.now())
                .set(OshOpenProjectSource::getLastSyncMessage, message));
    }

    private void ensureOwnerNotExists(String owner, Long excludeId) {
        LambdaQueryWrapper<OshOpenProjectSource> wrapper = new LambdaQueryWrapper<OshOpenProjectSource>()
                .eq(OshOpenProjectSource::getGithubOwner, owner)
                .eq(OshOpenProjectSource::getDeleteFlag, (byte) 0);
        if (excludeId != null) {
            wrapper.ne(OshOpenProjectSource::getId, excludeId);
        }
        if (sourceMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("GitHub项目源已存在");
        }
    }

    private String normalizeOwner(String owner, String url) {
        String value = StringUtils.hasText(owner) ? owner : url;
        if (!StringUtils.hasText(value)) {
            return null;
        }
        value = value.trim();
        value = value.replace("https://github.com/", "")
                .replace("http://github.com/", "")
                .replace("github.com/", "");
        int slash = value.indexOf('/');
        if (slash >= 0) {
            value = value.substring(0, slash);
        }
        return value.trim();
    }

    private String normalizeSourceType(String sourceType) {
        if ("org".equalsIgnoreCase(sourceType)) {
            return "org";
        }
        return "user";
    }

    private String trimToMax(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }
}

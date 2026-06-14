package com.backstage.system.service.openproject.impl;

import com.backstage.system.domain.openproject.OshOpenProject;
import com.backstage.system.domain.openproject.OshOpenProjectContributor;
import com.backstage.system.domain.openproject.OshOpenProjectResourceRel;
import com.backstage.system.domain.openproject.OshOpenProjectTag;
import com.backstage.system.domain.openproject.OshOpenProjectTagRel;
import com.backstage.system.domain.openproject.dto.OpenProjectContributorDTO;
import com.backstage.system.domain.openproject.dto.OpenProjectEditDTO;
import com.backstage.system.domain.openproject.dto.OpenProjectQueryDTO;
import com.backstage.system.domain.openproject.dto.OpenProjectResourceDTO;
import com.backstage.system.domain.openproject.vo.OpenProjectVO;
import com.backstage.system.domain.openproject.vo.OpenProjectResourceOptionVO;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.mapper.openproject.OshOpenProjectContributorMapper;
import com.backstage.system.mapper.openproject.OshOpenProjectMapper;
import com.backstage.system.mapper.openproject.OshOpenProjectResourceRelMapper;
import com.backstage.system.mapper.openproject.OshOpenProjectResourceSearchMapper;
import com.backstage.system.mapper.openproject.OshOpenProjectTagMapper;
import com.backstage.system.mapper.openproject.OshOpenProjectTagRelMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.openproject.IOshOpenProjectFavoriteService;
import com.backstage.system.service.openproject.IOshOpenProjectService;
import com.backstage.system.utils.UserContextUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OshOpenProjectServiceImpl implements IOshOpenProjectService {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_TEXT_LENGTH = 500;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_URL_LENGTH = 500;
    private static final int MAX_TAG_COUNT = 10;
    private static final int MAX_CUSTOM_TAG_LENGTH = 30;
    private static final int MAX_RESOURCE_COUNT = 20;
    private static final int MAX_CONTRIBUTOR_COUNT = 30;
    private static final Set<String> ALLOWED_RESOURCE_TYPES =
            new HashSet<>(Arrays.asList("course", "book", "tool"));

    @Autowired
    private OshOpenProjectMapper projectMapper;

    @Autowired
    private OshOpenProjectTagMapper tagMapper;

    @Autowired
    private OshOpenProjectTagRelMapper tagRelMapper;

    @Autowired
    private OshOpenProjectResourceRelMapper resourceRelMapper;

    @Autowired
    private OshOpenProjectResourceSearchMapper resourceSearchMapper;

    @Autowired
    private OshOpenProjectContributorMapper contributorMapper;

    @Autowired
    private OshUserMapper userMapper;

    @Autowired
    private IOshOpenProjectFavoriteService favoriteService;

    @Override
    public Map<String, Object> listPage(OpenProjectQueryDTO queryDTO) {
        queryDTO = normalizeQuery(queryDTO);
        int pageNum = queryDTO.getPageNum();
        int pageSize = queryDTO.getPageSize();

        Set<Long> tagFilterIds = getTagFilterProjectIds(queryDTO.getTagIds());
        if (tagFilterIds != null && tagFilterIds.isEmpty()) {
            return emptyPage(pageNum, pageSize);
        }

        Long currentUserId = UserContextUtil.getCurrentUserIdSafely();
        Set<Long> favoriteIds = null;
        if (Boolean.TRUE.equals(queryDTO.getOnlyFavorite())) {
            if (currentUserId == null) {
                return emptyPage(pageNum, pageSize);
            }
            favoriteIds = favoriteService.getFavoriteProjectIds(currentUserId);
            if (favoriteIds.isEmpty()) {
                return emptyPage(pageNum, pageSize);
            }
        }

        Set<Long> idFilter = mergeIdFilters(tagFilterIds, favoriteIds);
        if (idFilter != null && idFilter.isEmpty()) {
            return emptyPage(pageNum, pageSize);
        }

        LambdaQueryWrapper<OshOpenProject> wrapper = new LambdaQueryWrapper<OshOpenProject>()
                .eq(OshOpenProject::getStatus, 1)
                .eq(OshOpenProject::getDeleteFlag, (byte) 0);

        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String kw = "%" + queryDTO.getKeyword() + "%";
            wrapper.and(w -> w.like(OshOpenProject::getProjectName, kw)
                    .or().like(OshOpenProject::getProjectDesc, kw)
                    .or().like(OshOpenProject::getGithubOwner, kw)
                    .or().like(OshOpenProject::getGithubRepoName, kw));
        }
        if (idFilter != null) {
            wrapper.in(OshOpenProject::getId, idFilter);
        }
        if (queryDTO.getSourceId() != null && queryDTO.getSourceId() > 0) {
            wrapper.eq(OshOpenProject::getSourceId, queryDTO.getSourceId());
        }
        applySort(wrapper, queryDTO);

        PageHelper.startPage(pageNum, pageSize);
        List<OshOpenProject> projects = projectMapper.selectList(wrapper);
        PageInfo<OshOpenProject> pageInfo = new PageInfo<>(projects);

        List<OpenProjectVO> rows = buildProjectVOs(projects, currentUserId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("total", pageInfo.getTotal());
        result.put("pageNum", pageInfo.getPageNum());
        result.put("pageSize", pageInfo.getPageSize());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProject(OpenProjectEditDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("编辑内容不能为空");
        }
        if (dto.getId() == null) {
            throw new IllegalArgumentException("项目ID不能为空");
        }
        OshOpenProject project = projectMapper.selectOne(new LambdaQueryWrapper<OshOpenProject>()
                .eq(OshOpenProject::getId, dto.getId())
                .eq(OshOpenProject::getDeleteFlag, (byte) 0)
                .last("limit 1"));
        if (project == null) {
            throw new IllegalArgumentException("项目不存在");
        }
        if (project.getGithubRepoId() == null && !StringUtils.hasText(project.getGithubOwner())) {
            throw new IllegalArgumentException("只能编辑从 GitHub 数据源同步的开源项目");
        }

        String projectName = trimToMax(dto.getProjectName(), MAX_NAME_LENGTH);
        if (StringUtils.hasText(projectName)) {
            project.setProjectName(projectName);
        }
        project.setProjectDesc(trimToMax(dto.getProjectDesc(), MAX_TEXT_LENGTH));
        project.setAuthorName(trimToMax(dto.getAuthorName(), MAX_NAME_LENGTH));
        project.setProjectCover(trimToMax(dto.getProjectCover(), MAX_URL_LENGTH));
        project.setStatus(1);
        projectMapper.updateById(project);

        replaceProjectTags(project.getId(), dto.getTagIds(), dto.getCustomTags());
        replaceProjectResources(project.getId(), dto.getResources());
        replaceProjectContributors(project.getId(), dto.getContributors());
    }

    @Override
    public void incrementClickCount(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("项目ID不能为空");
        }
        int updated = projectMapper.update(null,
                new LambdaUpdateWrapper<OshOpenProject>()
                        .eq(OshOpenProject::getId, id)
                        .eq(OshOpenProject::getStatus, 1)
                        .eq(OshOpenProject::getDeleteFlag, (byte) 0)
                        .setSql("click_count = click_count + 1")
        );
        if (updated <= 0) {
            throw new IllegalArgumentException("项目不存在或未上线");
        }
    }

    @Override
    public OpenProjectVO getDetail(Long id) {
        if (id == null) {
            return null;
        }
        OshOpenProject project = projectMapper.selectById(id);
        if (project == null || project.getDeleteFlag() == 1 || !Integer.valueOf(1).equals(project.getStatus())) {
            return null;
        }
        List<OpenProjectVO> list = buildProjectVOs(Collections.singletonList(project), UserContextUtil.getCurrentUserIdSafely());
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<OshOpenProjectTag> listTags() {
        return tagMapper.selectList(
                new LambdaQueryWrapper<OshOpenProjectTag>()
                        .eq(OshOpenProjectTag::getDeleteFlag, (byte) 0)
                        .orderByAsc(OshOpenProjectTag::getSortOrder)
        );
    }

    private List<OpenProjectVO> buildProjectVOs(List<OshOpenProject> projects, Long currentUserId) {
        if (CollectionUtils.isEmpty(projects)) {
            return Collections.emptyList();
        }
        List<Long> projectIds = projects.stream().map(OshOpenProject::getId).collect(Collectors.toList());
        Map<Long, List<Long>> projectTagMap = new HashMap<>();
        Map<Long, List<String>> projectTagNameMap = new HashMap<>();
        Map<Long, List<OshOpenProjectResourceRel>> projectResourceMap = new HashMap<>();
        Map<Long, List<OshOpenProjectContributor>> projectContributorMap = new HashMap<>();

        List<OshOpenProjectTagRel> rels = tagRelMapper.selectList(
                new LambdaQueryWrapper<OshOpenProjectTagRel>().in(OshOpenProjectTagRel::getProjectId, projectIds));
        List<OshOpenProjectTag> allTags = tagMapper.selectList(
                new LambdaQueryWrapper<OshOpenProjectTag>().eq(OshOpenProjectTag::getDeleteFlag, (byte) 0));
        Map<Long, String> tagIdNameMap = allTags.stream()
                .collect(Collectors.toMap(OshOpenProjectTag::getId, OshOpenProjectTag::getTagName, (a, b) -> a));
        for (OshOpenProjectTagRel rel : rels) {
            projectTagMap.computeIfAbsent(rel.getProjectId(), k -> new ArrayList<>()).add(rel.getTagId());
            String tagName = tagIdNameMap.get(rel.getTagId());
            if (tagName != null) {
                projectTagNameMap.computeIfAbsent(rel.getProjectId(), k -> new ArrayList<>()).add(tagName);
            }
        }

        List<OshOpenProjectResourceRel> resourceRels = resourceRelMapper.selectList(
                new LambdaQueryWrapper<OshOpenProjectResourceRel>()
                        .in(OshOpenProjectResourceRel::getProjectId, projectIds)
                        .eq(OshOpenProjectResourceRel::getDeleteFlag, (byte) 0));
        for (OshOpenProjectResourceRel rel : resourceRels) {
            projectResourceMap.computeIfAbsent(rel.getProjectId(), k -> new ArrayList<>()).add(rel);
        }

        List<OshOpenProjectContributor> contributors = contributorMapper.selectList(
                new LambdaQueryWrapper<OshOpenProjectContributor>()
                        .in(OshOpenProjectContributor::getProjectId, projectIds)
                        .eq(OshOpenProjectContributor::getDeleteFlag, (byte) 0)
                        .orderByAsc(OshOpenProjectContributor::getSortOrder));
        fillContributorDisplayInfo(contributors);
        for (OshOpenProjectContributor contributor : contributors) {
            projectContributorMap.computeIfAbsent(contributor.getProjectId(), k -> new ArrayList<>()).add(contributor);
        }

        Set<Long> userFavoriteIds = favoriteService.getFavoriteProjectIds(currentUserId);
        return projects.stream().map(project -> {
            OpenProjectVO vo = new OpenProjectVO();
            vo.setId(project.getId());
            vo.setProjectName(project.getProjectName());
            vo.setProjectDesc(project.getProjectDesc());
            vo.setProjectUrl(project.getProjectUrl());
            vo.setAuthorName(project.getAuthorName());
            vo.setProjectCover(project.getProjectCover());
            vo.setStatus(project.getStatus());
            vo.setClickCount(project.getClickCount());
            vo.setCreateTime(project.getCreateTime());
            vo.setTagIds(projectTagMap.getOrDefault(project.getId(), Collections.emptyList()));
            vo.setTagNames(projectTagNameMap.getOrDefault(project.getId(), Collections.emptyList()));
            vo.setStarCount(project.getStarCount());
            vo.setForkCount(project.getForkCount());
            vo.setLastCommitTime(project.getLastCommitTime());
            vo.setIsArchived(project.getIsArchived());
            vo.setLastSyncTime(project.getLastSyncTime());
            fillGithubFields(vo, project);
            vo.setResources(projectResourceMap.getOrDefault(project.getId(), Collections.emptyList()));
            vo.setContributors(projectContributorMap.getOrDefault(project.getId(), Collections.emptyList()));
            vo.setFavorited(userFavoriteIds.contains(project.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    private void fillContributorDisplayInfo(List<OshOpenProjectContributor> contributors) {
        if (CollectionUtils.isEmpty(contributors)) {
            return;
        }
        for (OshOpenProjectContributor contributor : contributors) {
            String githubAccount = contributor.getGithubAccount();
            if (!StringUtils.hasText(githubAccount)) {
                continue;
            }
            if (!StringUtils.hasText(contributor.getWechatName())) {
                contributor.setWechatName(resolveWechatName(githubAccount));
            }
            if (!StringUtils.hasText(contributor.getProfileUrl())) {
                contributor.setProfileUrl(resolveGithubProfileUrl(githubAccount, contributor.getProfileUrl()));
            }
        }
    }

    private Set<Long> getTagFilterProjectIds(List<Long> tagIds) {
        Set<Long> queryTagIds = normalizeTagIds(tagIds);
        if (queryTagIds.isEmpty()) {
            return null;
        }
        List<OshOpenProjectTagRel> rels = tagRelMapper.selectList(
                new LambdaQueryWrapper<OshOpenProjectTagRel>().in(OshOpenProjectTagRel::getTagId, queryTagIds));
        return rels.stream().map(OshOpenProjectTagRel::getProjectId).collect(Collectors.toSet());
    }

    private Set<Long> mergeIdFilters(Set<Long> first, Set<Long> second) {
        if (first != null && second != null) {
            return first.stream().filter(second::contains).collect(Collectors.toSet());
        }
        return first != null ? first : second;
    }

    private void applySort(LambdaQueryWrapper<OshOpenProject> wrapper, OpenProjectQueryDTO queryDTO) {
        boolean asc = "asc".equalsIgnoreCase(queryDTO.getSortOrder());
        switch (queryDTO.getSortField() == null ? "" : queryDTO.getSortField()) {
            case "star_count":
                if (asc) wrapper.orderByAsc(OshOpenProject::getStarCount);
                else wrapper.orderByDesc(OshOpenProject::getStarCount);
                break;
            case "fork_count":
                if (asc) wrapper.orderByAsc(OshOpenProject::getForkCount);
                else wrapper.orderByDesc(OshOpenProject::getForkCount);
                break;
            case "last_commit_time":
                if (asc) wrapper.orderByAsc(OshOpenProject::getLastCommitTime);
                else wrapper.orderByDesc(OshOpenProject::getLastCommitTime);
                break;
            default:
                if (asc) wrapper.orderByAsc(OshOpenProject::getCreateTime);
                else wrapper.orderByDesc(OshOpenProject::getCreateTime);
        }
    }

    private void replaceProjectTags(Long projectId, List<Long> tagIds, List<String> customTags) {
        tagRelMapper.delete(new LambdaQueryWrapper<OshOpenProjectTagRel>()
                .eq(OshOpenProjectTagRel::getProjectId, projectId));

        Set<Long> allTagIds = new LinkedHashSet<>();
        Set<Long> normalizedTagIds = normalizeTagIds(tagIds);
        if (!normalizedTagIds.isEmpty()) {
            List<OshOpenProjectTag> existingTags = tagMapper.selectList(
                    new LambdaQueryWrapper<OshOpenProjectTag>()
                            .in(OshOpenProjectTag::getId, normalizedTagIds)
                            .eq(OshOpenProjectTag::getDeleteFlag, (byte) 0));
            existingTags.forEach(tag -> allTagIds.add(tag.getId()));
        }

        for (String tagName : normalizeCustomTags(customTags)) {
            allTagIds.add(findOrCreateTag(tagName));
            if (allTagIds.size() >= MAX_TAG_COUNT) {
                break;
            }
        }

        for (Long tagId : allTagIds) {
            OshOpenProjectTagRel rel = new OshOpenProjectTagRel();
            rel.setProjectId(projectId);
            rel.setTagId(tagId);
            tagRelMapper.insert(rel);
        }
    }

    private Long findOrCreateTag(String tagName) {
        OshOpenProjectTag existing = tagMapper.selectOne(
                new LambdaQueryWrapper<OshOpenProjectTag>()
                        .eq(OshOpenProjectTag::getTagName, tagName)
                        .eq(OshOpenProjectTag::getDeleteFlag, (byte) 0)
                        .last("limit 1"));
        if (existing != null) {
            return existing.getId();
        }
        OshOpenProjectTag tag = new OshOpenProjectTag();
        tag.setTagName(tagName);
        tag.setTagCode(tagName.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_"));
        tag.setSortOrder(999);
        tag.setDeleted(false);
        tagMapper.insert(tag);
        return tag.getId();
    }

    private void replaceProjectResources(Long projectId, List<OpenProjectResourceDTO> resources) {
        resourceRelMapper.update(null, new LambdaUpdateWrapper<OshOpenProjectResourceRel>()
                .eq(OshOpenProjectResourceRel::getProjectId, projectId)
                .set(OshOpenProjectResourceRel::getDeleteFlag, (byte) 1));

        for (OpenProjectResourceDTO item : limitList(resources, MAX_RESOURCE_COUNT)) {
            if (item == null) {
                continue;
            }
            String resourceType = normalizeResourceType(item.getResourceType());
            Long resourceId = item.getResourceId();
            if (resourceId == null) {
                continue;
            }
            OpenProjectResourceOptionVO option = resolveResourceOption(resourceType, resourceId);
            if (option == null) {
                continue;
            }
            OshOpenProjectResourceRel rel = new OshOpenProjectResourceRel();
            rel.setProjectId(projectId);
            rel.setResourceType(resourceType);
            rel.setResourceId(resourceId);
            rel.setResourceUrl(trimToMax(option.getResourceUrl(), MAX_URL_LENGTH));
            rel.setResourceName(trimToMax(option.getResourceName(), 200));
            rel.setDeleted(false);
            resourceRelMapper.insert(rel);
        }
    }

    private OpenProjectResourceOptionVO resolveResourceOption(String resourceType, Long resourceId) {
        if (resourceId == null) {
            return null;
        }
        if ("book".equals(resourceType)) {
            return resourceSearchMapper.selectBookById(resourceId);
        }
        if ("tool".equals(resourceType)) {
            return resourceSearchMapper.selectToolById(resourceId);
        }
        return resourceSearchMapper.selectCourseById(resourceId);
    }

    private void replaceProjectContributors(Long projectId, List<OpenProjectContributorDTO> contributors) {
        if (contributors == null) {
            return;
        }
        contributorMapper.update(null, new LambdaUpdateWrapper<OshOpenProjectContributor>()
                .eq(OshOpenProjectContributor::getProjectId, projectId)
                .set(OshOpenProjectContributor::getDeleteFlag, (byte) 1));

        int sort = 0;
        for (OpenProjectContributorDTO item : limitList(contributors, MAX_CONTRIBUTOR_COUNT)) {
            if (item == null || !StringUtils.hasText(item.getGithubAccount())) {
                continue;
            }
            String githubAccount = normalizeGithubAccount(item.getGithubAccount());
            if (!StringUtils.hasText(githubAccount)) {
                continue;
            }
            OshOpenProjectContributor contributor = contributorMapper.selectOne(
                    new LambdaQueryWrapper<OshOpenProjectContributor>()
                            .eq(OshOpenProjectContributor::getProjectId, projectId)
                            .eq(OshOpenProjectContributor::getGithubAccount, githubAccount)
                            .last("limit 1"));
            if (contributor == null) {
                contributor = new OshOpenProjectContributor();
                contributor.setProjectId(projectId);
                contributor.setGithubAccount(githubAccount);
            }
            String wechatName = trimToMax(item.getWechatName(), MAX_NAME_LENGTH);
            if (!StringUtils.hasText(wechatName)) {
                wechatName = resolveWechatName(githubAccount);
            }
            contributor.setWechatName(wechatName);
            contributor.setContributorType(normalizeContributorType(item.getContributorType(), sort));
            if (contributor.getId() == null) {
                contributor.setContributions(0);
                contributor.setAvatarUrl(trimToMax(item.getAvatarUrl(), MAX_URL_LENGTH));
                contributor.setProfileUrl(resolveGithubProfileUrl(githubAccount, null));
            }
            contributor.setSource("manual");
            contributor.setEditable(1);
            contributor.setSortOrder(item.getSortOrder() == null ? sort : item.getSortOrder());
            contributor.setDeleted(false);
            if (contributor.getId() == null) {
                contributorMapper.insert(contributor);
            } else {
                contributorMapper.updateById(contributor);
            }
            sort++;
        }
    }

    private String resolveWechatName(String githubAccount) {
        if (!StringUtils.hasText(githubAccount)) {
            return null;
        }
        String normalizedAccount = normalizeGithubAccount(githubAccount);
        OshUser user = userMapper.selectOne(new LambdaQueryWrapper<OshUser>()
                .eq(OshUser::getDeleteFlag, (byte) 0)
                .and(wrapper -> wrapper
                        .eq(OshUser::getGithubAccount, normalizedAccount)
                        .or()
                        .eq(OshUser::getGithubAccount, "https://github.com/" + normalizedAccount)
                        .or()
                        .eq(OshUser::getGithubAccount, "http://github.com/" + normalizedAccount)
                        .or()
                        .eq(OshUser::getGithubAccount, "github.com/" + normalizedAccount))
                .last("limit 1"));
        return user == null ? null : trimToMax(user.getWechatName(), MAX_NAME_LENGTH);
    }

    private String resolveGithubProfileUrl(String githubAccount, String profileUrl) {
        String normalized = trimToMax(profileUrl, MAX_URL_LENGTH);
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        return "https://github.com/" + githubAccount;
    }

    private String normalizeGithubAccount(String githubAccount) {
        String normalized = trimToMax(githubAccount, MAX_NAME_LENGTH);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        normalized = normalized.replace("https://github.com/", "")
                .replace("http://github.com/", "")
                .replace("github.com/", "");
        int slashIndex = normalized.indexOf('/');
        if (slashIndex >= 0) {
            normalized = normalized.substring(0, slashIndex);
        }
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        return trimToMax(normalized, MAX_NAME_LENGTH);
    }

    private String normalizeContributorType(String contributorType, int sort) {
        String normalized = trimToMax(contributorType, 20);
        if ("primary".equalsIgnoreCase(normalized)) {
            return "primary";
        }
        if ("collaborator".equalsIgnoreCase(normalized)) {
            return "collaborator";
        }
        return sort == 0 ? "primary" : "contributor";
    }

    private void fillGithubFields(OpenProjectVO vo, OshOpenProject p) {
        vo.setSourceId(p.getSourceId());
        vo.setGithubRepoId(p.getGithubRepoId());
        vo.setGithubOwner(p.getGithubOwner());
        vo.setGithubRepoName(p.getGithubRepoName());
        vo.setDefaultBranch(p.getDefaultBranch());
        vo.setLanguage(p.getLanguage());
        vo.setLicenseName(p.getLicenseName());
        vo.setHomepage(p.getHomepage());
    }

    private OpenProjectQueryDTO normalizeQuery(OpenProjectQueryDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new OpenProjectQueryDTO();
        }
        Integer pageNum = queryDTO.getPageNum();
        Integer pageSize = queryDTO.getPageSize();
        queryDTO.setPageNum(pageNum == null || pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum);
        queryDTO.setPageSize(pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE));
        queryDTO.setKeyword(trimToMax(queryDTO.getKeyword(), MAX_NAME_LENGTH));
        return queryDTO;
    }

    private Map<String, Object> emptyPage(int pageNum, int pageSize) {
        Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("rows", Collections.emptyList());
        empty.put("total", 0L);
        empty.put("pageNum", pageNum);
        empty.put("pageSize", pageSize);
        return empty;
    }

    private String normalizeResourceType(String resourceType) {
        String normalized = trimToMax(resourceType, 20);
        if (normalized == null) {
            return "tool";
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        return ALLOWED_RESOURCE_TYPES.contains(normalized) ? normalized : "tool";
    }

    private Set<Long> normalizeTagIds(List<Long> tagIds) {
        if (CollectionUtils.isEmpty(tagIds)) {
            return Collections.emptySet();
        }
        return tagIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .limit(MAX_TAG_COUNT)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> normalizeCustomTags(List<String> customTags) {
        if (CollectionUtils.isEmpty(customTags)) {
            return Collections.emptySet();
        }
        return customTags.stream()
                .map(tag -> trimToMax(tag, MAX_CUSTOM_TAG_LENGTH))
                .filter(StringUtils::hasText)
                .limit(MAX_TAG_COUNT)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private <T> List<T> limitList(List<T> source, int limit) {
        if (CollectionUtils.isEmpty(source)) {
            return Collections.emptyList();
        }
        return source.stream().limit(limit).collect(Collectors.toList());
    }

    private String trimToMax(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }
}

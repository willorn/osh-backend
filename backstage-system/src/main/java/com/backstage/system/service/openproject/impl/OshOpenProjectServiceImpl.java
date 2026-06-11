package com.backstage.system.service.openproject.impl;

import com.backstage.system.domain.openproject.OshOpenProject;
import com.backstage.system.domain.openproject.OshOpenProjectResourceRel;
import com.backstage.system.domain.openproject.OshOpenProjectTag;
import com.backstage.system.domain.openproject.OshOpenProjectTagRel;
import com.backstage.system.domain.openproject.dto.OpenProjectAuditDTO;
import com.backstage.system.domain.openproject.dto.OpenProjectQueryDTO;
import com.backstage.system.domain.openproject.dto.OpenProjectResourceDTO;
import com.backstage.system.domain.openproject.dto.OpenProjectSubmitDTO;
import com.backstage.system.domain.openproject.vo.OpenProjectVO;
import com.backstage.system.domain.websocket.WsNotifyMessage;
import com.backstage.system.enums.behavior.ContributionResourceType;
import com.backstage.system.mapper.openproject.OshOpenProjectMapper;
import com.backstage.system.mapper.openproject.OshOpenProjectResourceRelMapper;
import com.backstage.system.mapper.openproject.OshOpenProjectTagMapper;
import com.backstage.system.mapper.openproject.OshOpenProjectTagRelMapper;
import com.backstage.system.service.behavior.ContributionService;
import com.backstage.system.service.openproject.IOshOpenProjectFavoriteService;
import com.backstage.system.service.openproject.IOshOpenProjectService;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import com.backstage.system.utils.UserContextUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
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
    private IOshOpenProjectFavoriteService favoriteService;

    @Autowired
    private WebSocketNotifyService webSocketNotifyService;

    @Autowired
    private ContributionService contributionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> listPage(OpenProjectQueryDTO queryDTO) {
        queryDTO = normalizeQuery(queryDTO);
        int pageNum = queryDTO.getPageNum();
        int pageSize = queryDTO.getPageSize();

        // 如果按标签筛选，先查出符合标签的 projectId
        Set<Long> tagFilterIds = null;
        Set<Long> queryTagIds = normalizeTagIds(queryDTO.getTagIds());
        if (!queryTagIds.isEmpty()) {
            List<OshOpenProjectTagRel> rels = tagRelMapper.selectList(
                    new LambdaQueryWrapper<OshOpenProjectTagRel>()
                            .in(OshOpenProjectTagRel::getTagId, queryTagIds)
            );
            tagFilterIds = rels.stream().map(OshOpenProjectTagRel::getProjectId).collect(Collectors.toSet());
            if (tagFilterIds.isEmpty()) {
                Map<String, Object> empty = new LinkedHashMap<>();
                empty.put("rows", Collections.emptyList());
                empty.put("total", 0L);
                empty.put("pageNum", pageNum);
                empty.put("pageSize", pageSize);
                return empty;
            }
        }

        // 如果只看收藏，查出当前用户收藏的 projectId
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

        // 合并标签筛选和收藏筛选的 id 集合（取交集）
        Set<Long> idFilter = null;
        if (tagFilterIds != null && favoriteIds != null) {
            idFilter = tagFilterIds.stream().filter(favoriteIds::contains).collect(Collectors.toSet());
            if (idFilter.isEmpty()) {
                Map<String, Object> empty = new LinkedHashMap<>();
                empty.put("rows", Collections.emptyList());
                empty.put("total", 0L);
                empty.put("pageNum", pageNum);
                empty.put("pageSize", pageSize);
                return empty;
            }
        } else if (tagFilterIds != null) {
            idFilter = tagFilterIds;
        } else if (favoriteIds != null) {
            idFilter = favoriteIds;
        }

        // 构建查询条件
        LambdaQueryWrapper<OshOpenProject> wrapper = new LambdaQueryWrapper<OshOpenProject>()
                .eq(OshOpenProject::getStatus, 1)
                .eq(OshOpenProject::getDeleteFlag, (byte) 0);

        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String kw = "%" + queryDTO.getKeyword() + "%";
            wrapper.and(w -> w.like(OshOpenProject::getProjectName, kw)
                    .or().like(OshOpenProject::getProjectDesc, kw));
        }

        if (idFilter != null) {
            wrapper.in(OshOpenProject::getId, idFilter);
        }
        // 排序：白名单校验，防止 SQL 注入
        String sortField = queryDTO.getSortField();
        boolean asc = "asc".equalsIgnoreCase(queryDTO.getSortOrder());
        switch (sortField == null ? "" : sortField) {
            case "star_count":
                if (asc) wrapper.orderByAsc(OshOpenProject::getStarCount);
                else     wrapper.orderByDesc(OshOpenProject::getStarCount);
                break;
            case "fork_count":
                if (asc) wrapper.orderByAsc(OshOpenProject::getForkCount);
                else     wrapper.orderByDesc(OshOpenProject::getForkCount);
                break;
            case "last_commit_time":
                if (asc) wrapper.orderByAsc(OshOpenProject::getLastCommitTime);
                else     wrapper.orderByDesc(OshOpenProject::getLastCommitTime);
                break;
            default:
                wrapper.orderByDesc(OshOpenProject::getCreateTime);
        }

        PageHelper.startPage(pageNum, pageSize);
        List<OshOpenProject> projects = projectMapper.selectList(wrapper);
        PageInfo<OshOpenProject> pageInfo = new PageInfo<>(projects);

        // 批量查询标签关联
        List<Long> projectIds = projects.stream().map(OshOpenProject::getId).collect(Collectors.toList());
        Map<Long, List<Long>> projectTagMap = new HashMap<>();
        Map<Long, List<String>> projectTagNameMap = new HashMap<>();

        if (!projectIds.isEmpty()) {
            List<OshOpenProjectTagRel> rels = tagRelMapper.selectList(
                    new LambdaQueryWrapper<OshOpenProjectTagRel>()
                            .in(OshOpenProjectTagRel::getProjectId, projectIds)
            );
            // 查所有标签
            List<OshOpenProjectTag> allTags = tagMapper.selectList(
                    new LambdaQueryWrapper<OshOpenProjectTag>().eq(OshOpenProjectTag::getDeleteFlag, (byte) 0)
            );
            Map<Long, String> tagIdNameMap = allTags.stream()
                    .collect(Collectors.toMap(OshOpenProjectTag::getId, OshOpenProjectTag::getTagName));

            for (OshOpenProjectTagRel rel : rels) {
                projectTagMap.computeIfAbsent(rel.getProjectId(), k -> new ArrayList<>()).add(rel.getTagId());
                String tagName = tagIdNameMap.get(rel.getTagId());
                if (tagName != null) {
                    projectTagNameMap.computeIfAbsent(rel.getProjectId(), k -> new ArrayList<>()).add(tagName);
                }
            }
        }

        // 查当前用户收藏集合（用于标记 favorited，未登录则为空集合）
        final Set<Long> userFavoriteIds = favoriteService.getFavoriteProjectIds(currentUserId);

        // 批量查询资源关联
        Map<Long, List<OshOpenProjectResourceRel>> projectResourceMap = new HashMap<>();
        if (!projectIds.isEmpty()) {
            List<OshOpenProjectResourceRel> resourceRels = resourceRelMapper.selectList(
                    new LambdaQueryWrapper<OshOpenProjectResourceRel>()
                            .in(OshOpenProjectResourceRel::getProjectId, projectIds)
                            .eq(OshOpenProjectResourceRel::getDeleteFlag, (byte) 0)
            );
            for (OshOpenProjectResourceRel rel : resourceRels) {
                projectResourceMap.computeIfAbsent(rel.getProjectId(), k -> new ArrayList<>()).add(rel);
            }
        }

        // 转 VO
        List<OpenProjectVO> voList = projects.stream().map(p -> {
            OpenProjectVO vo = new OpenProjectVO();
            vo.setId(p.getId());
            vo.setProjectName(p.getProjectName());
            vo.setProjectDesc(p.getProjectDesc());
            vo.setProjectUrl(p.getProjectUrl());
            vo.setAuthorName(p.getAuthorName());
            vo.setProjectCover(p.getProjectCover());
            vo.setStatus(p.getStatus());
            vo.setClickCount(p.getClickCount());
            vo.setCreateTime(p.getCreateTime());
            vo.setTagIds(projectTagMap.getOrDefault(p.getId(), Collections.emptyList()));
            vo.setTagNames(projectTagNameMap.getOrDefault(p.getId(), Collections.emptyList()));
            vo.setStarCount(p.getStarCount());
            vo.setForkCount(p.getForkCount());
            vo.setLastCommitTime(p.getLastCommitTime());
            vo.setIsArchived(p.getIsArchived());
            vo.setLastSyncTime(p.getLastSyncTime());
            vo.setResources(projectResourceMap.getOrDefault(p.getId(), Collections.emptyList()));
            vo.setFavorited(userFavoriteIds.contains(p.getId()));
            return vo;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", voList);
        result.put("total", pageInfo.getTotal());
        result.put("pageNum", pageInfo.getPageNum());
        result.put("pageSize", pageInfo.getPageSize());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(OpenProjectSubmitDTO dto) {
        if (dto == null) throw new IllegalArgumentException("提交内容不能为空");
        String projectName = trimToMax(dto.getProjectName(), MAX_NAME_LENGTH);
        String projectUrl = trimToMax(dto.getProjectUrl(), MAX_URL_LENGTH);
        if (!StringUtils.hasText(projectName)) throw new IllegalArgumentException("项目名称不能为空");
        if (!StringUtils.hasText(projectUrl)) throw new IllegalArgumentException("项目链接不能为空");
        if (!projectUrl.matches("^https?://[^\\s]+$")) throw new IllegalArgumentException("请输入有效的URL地址");

        OshOpenProject project = new OshOpenProject();
        project.setProjectName(projectName);
        project.setProjectDesc(trimToMax(dto.getProjectDesc(), MAX_TEXT_LENGTH));
        project.setProjectUrl(projectUrl);
        project.setAuthorName(trimToMax(dto.getAuthorName(), MAX_NAME_LENGTH));
        project.setProjectCover(trimToMax(dto.getProjectCover(), MAX_URL_LENGTH));
        project.setStatus(0);
        project.setClickCount(0);
        project.setStarCount(0);
        project.setForkCount(0);
        project.setIsArchived((byte) 0);
        project.setDeleted(false);
        projectMapper.insert(project);
        contributionService.recordContribution(ContributionResourceType.OPEN_PROJECT.getCode(), project.getId(), projectName);

        // 保存资源关联（课程、电子书、工具等）
        List<OpenProjectResourceDTO> resources = limitList(dto.getResources(), MAX_RESOURCE_COUNT);
        if (!CollectionUtils.isEmpty(resources)) {
            for (OpenProjectResourceDTO res : resources) {
                if (res == null) continue;
                String resourceType = normalizeResourceType(res.getResourceType());
                String resourceUrl = trimToMax(res.getResourceUrl(), MAX_URL_LENGTH);
                if (!StringUtils.hasText(resourceUrl)) continue;
                OshOpenProjectResourceRel rel = new OshOpenProjectResourceRel();
                rel.setProjectId(project.getId());
                rel.setResourceType(resourceType);
                rel.setResourceUrl(resourceUrl);
                rel.setResourceName(trimToMax(res.getResourceName(), MAX_NAME_LENGTH));
                rel.setDeleted(false);
                resourceRelMapper.insert(rel);
            }
        }

        // 保存已有标签关联
        Set<Long> tagIds = normalizeTagIds(dto.getTagIds());
        if (!tagIds.isEmpty()) {
            List<OshOpenProjectTag> existingTags = tagMapper.selectList(
                    new LambdaQueryWrapper<OshOpenProjectTag>()
                            .in(OshOpenProjectTag::getId, tagIds)
                            .eq(OshOpenProjectTag::getDeleteFlag, (byte) 0)
            );
            for (OshOpenProjectTag tag : existingTags) {
                OshOpenProjectTagRel rel = new OshOpenProjectTagRel();
                rel.setProjectId(project.getId());
                rel.setTagId(tag.getId());
                tagRelMapper.insert(rel);
            }
        }

        // 处理自定义标签：不存在则创建，然后建立关联
        Set<String> customTags = normalizeCustomTags(dto.getCustomTags());
        if (!customTags.isEmpty()) {
            for (String trimmed : customTags) {

                // 查询标签是否已存在（忽略大小写）
                OshOpenProjectTag existTag = tagMapper.selectOne(
                        new LambdaQueryWrapper<OshOpenProjectTag>()
                                .eq(OshOpenProjectTag::getTagName, trimmed)
                                .eq(OshOpenProjectTag::getDeleteFlag, (byte) 0)
                                .last("limit 1")
                );

                Long tagId;
                if (existTag != null) {
                    // 已存在，直接复用
                    tagId = existTag.getId();
                } else {
                    // 不存在，创建新标签
                    OshOpenProjectTag newTag = new OshOpenProjectTag();
                    newTag.setTagName(trimmed);
                    newTag.setTagCode(trimmed.toLowerCase().replaceAll("\\s+", "_"));
                    newTag.setSortOrder(999);
                    newTag.setDeleted(false);
                    tagMapper.insert(newTag);
                    tagId = newTag.getId();
                }

                OshOpenProjectTagRel rel = new OshOpenProjectTagRel();
                rel.setProjectId(project.getId());
                rel.setTagId(tagId);
                tagRelMapper.insert(rel);
            }
        }
    }

    @Override
    public Map<String, Object> listPending(OpenProjectQueryDTO queryDTO) {
        queryDTO = normalizeQuery(queryDTO);
        int pageNum = queryDTO.getPageNum();
        int pageSize = queryDTO.getPageSize();

        LambdaQueryWrapper<OshOpenProject> wrapper = new LambdaQueryWrapper<OshOpenProject>()
                .eq(OshOpenProject::getStatus, 0)
                .eq(OshOpenProject::getDeleteFlag, (byte) 0)
                .orderByAsc(OshOpenProject::getCreateTime);

        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String kw = "%" + queryDTO.getKeyword() + "%";
            wrapper.and(w -> w.like(OshOpenProject::getProjectName, kw)
                    .or().like(OshOpenProject::getProjectDesc, kw));
        }

        PageHelper.startPage(pageNum, pageSize);
        List<OshOpenProject> projects = projectMapper.selectList(wrapper);
        PageInfo<OshOpenProject> pageInfo = new PageInfo<>(projects);

        // 批量查询标签关联
        List<Long> projectIds = projects.stream().map(OshOpenProject::getId).collect(Collectors.toList());
        Map<Long, List<String>> projectTagNameMap = new HashMap<>();
        if (!projectIds.isEmpty()) {
            List<OshOpenProjectTagRel> rels = tagRelMapper.selectList(
                    new LambdaQueryWrapper<OshOpenProjectTagRel>()
                            .in(OshOpenProjectTagRel::getProjectId, projectIds)
            );
            List<OshOpenProjectTag> allTags = tagMapper.selectList(
                    new LambdaQueryWrapper<OshOpenProjectTag>().eq(OshOpenProjectTag::getDeleteFlag, (byte) 0)
            );
            Map<Long, String> tagIdNameMap = allTags.stream()
                    .collect(Collectors.toMap(OshOpenProjectTag::getId, OshOpenProjectTag::getTagName));
            for (OshOpenProjectTagRel rel : rels) {
                String tagName = tagIdNameMap.get(rel.getTagId());
                if (tagName != null) {
                    projectTagNameMap.computeIfAbsent(rel.getProjectId(), k -> new ArrayList<>()).add(tagName);
                }
            }
        }

        List<OpenProjectVO> voList = projects.stream().map(p -> {
            OpenProjectVO vo = new OpenProjectVO();
            vo.setId(p.getId());
            vo.setProjectName(p.getProjectName());
            vo.setProjectDesc(p.getProjectDesc());
            vo.setProjectUrl(p.getProjectUrl());
            vo.setAuthorName(p.getAuthorName());
            vo.setProjectCover(p.getProjectCover());
            vo.setStatus(p.getStatus());
            vo.setCreateTime(p.getCreateTime());
            vo.setTagNames(projectTagNameMap.getOrDefault(p.getId(), Collections.emptyList()));
            return vo;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", voList);
        result.put("total", pageInfo.getTotal());
        result.put("pageNum", pageInfo.getPageNum());
        result.put("pageSize", pageInfo.getPageSize());
        return result;
    }

    @Override
    public void audit(OpenProjectAuditDTO dto) {
        if (dto == null) throw new IllegalArgumentException("审核内容不能为空");
        if (dto.getId() == null) throw new IllegalArgumentException("项目ID不能为空");
        if (dto.getStatus() == null || (dto.getStatus() != 1 && dto.getStatus() != 2)) {
            throw new IllegalArgumentException("审核状态不合法");
        }
        if (dto.getStatus() == 2 && !StringUtils.hasText(dto.getRejectReason())) {
            throw new IllegalArgumentException("拒绝时必须填写原因");
        }

        OshOpenProject project = projectMapper.selectById(dto.getId());
        if (project == null) throw new IllegalArgumentException("项目不存在");
        Integer oldStatus = project.getStatus();

        project.setStatus(dto.getStatus());
        project.setRejectReason(trimToMax(dto.getRejectReason(), MAX_TEXT_LENGTH));
        projectMapper.updateById(project);

        // 审核通过时广播公告给所有在线用户，并写入公告表
        if (dto.getStatus() == 1 && !Integer.valueOf(1).equals(oldStatus)) {
            String announcementTitle = "「" + project.getProjectName() + "」已上线，快来看看吧！";

            // 写入 osh_announcement 表
            jdbcTemplate.update(
                "INSERT INTO osh_announcement (title, link, resource_type, channel, status, delete_flag, create_by, create_time, update_by, update_time) " +
                "VALUES (?, ?, 'openproject', 0, 0, 0, 'system', NOW(), 'system', NOW())",
                announcementTitle, "/openproject/list"
            );

            // WebSocket 广播
            WsNotifyMessage broadcast = new WsNotifyMessage();
            broadcast.setType("NEW_OPEN_PROJECT");
            broadcast.setTitle("新开源项目上线");
            broadcast.setContent(announcementTitle);
            broadcast.setJumpUrl("/openproject/list");
            broadcast.setBizId(String.valueOf(project.getId()));
            webSocketNotifyService.broadcast(broadcast);
        }
    }

    @Override
    public void incrementClickCount(Long id) {
        if (id == null) throw new IllegalArgumentException("项目ID不能为空");
        int updated = projectMapper.update(null,
                new LambdaUpdateWrapper<OshOpenProject>()
                        .eq(OshOpenProject::getId, id)
                        .eq(OshOpenProject::getStatus, 1)
                        .eq(OshOpenProject::getDeleteFlag, (byte) 0)
                        .setSql("click_count = click_count + 1")
        );
        if (updated <= 0) throw new IllegalArgumentException("项目不存在或未上线");
    }

    @Override
    public OpenProjectVO getDetail(Long id) {
        if (id == null) return null;
        OshOpenProject p = projectMapper.selectById(id);
        if (p == null || p.getDeleteFlag() == 1 || !Integer.valueOf(1).equals(p.getStatus())) return null;

        // 查标签
        List<OshOpenProjectTagRel> rels = tagRelMapper.selectList(
                new LambdaQueryWrapper<OshOpenProjectTagRel>()
                        .eq(OshOpenProjectTagRel::getProjectId, id)
        );
        List<Long> tagIds = rels.stream().map(OshOpenProjectTagRel::getTagId).collect(Collectors.toList());
        List<String> tagNames = Collections.emptyList();
        if (!tagIds.isEmpty()) {
            List<OshOpenProjectTag> tags = tagMapper.selectList(
                    new LambdaQueryWrapper<OshOpenProjectTag>()
                            .in(OshOpenProjectTag::getId, tagIds)
                            .eq(OshOpenProjectTag::getDeleteFlag, (byte) 0)
            );
            tagNames = tags.stream().map(OshOpenProjectTag::getTagName).collect(Collectors.toList());
        }

        OpenProjectVO vo = new OpenProjectVO();
        vo.setId(p.getId());
        vo.setProjectName(p.getProjectName());
        vo.setProjectDesc(p.getProjectDesc());
        vo.setProjectUrl(p.getProjectUrl());
        vo.setAuthorName(p.getAuthorName());
        vo.setProjectCover(p.getProjectCover());
        vo.setStatus(p.getStatus());
        vo.setClickCount(p.getClickCount());
        vo.setCreateTime(p.getCreateTime());
        vo.setTagIds(tagIds);
        vo.setTagNames(tagNames);
        vo.setStarCount(p.getStarCount());
        vo.setForkCount(p.getForkCount());
        vo.setLastCommitTime(p.getLastCommitTime());
        vo.setIsArchived(p.getIsArchived());
        vo.setLastSyncTime(p.getLastSyncTime());

        // 查资源关联
        List<OshOpenProjectResourceRel> resources = resourceRelMapper.selectList(
                new LambdaQueryWrapper<OshOpenProjectResourceRel>()
                        .eq(OshOpenProjectResourceRel::getProjectId, id)
                        .eq(OshOpenProjectResourceRel::getDeleteFlag, (byte) 0)
        );
        vo.setResources(resources);
        return vo;
    }

    @Override
    public List<OshOpenProjectTag> listTags() {
        return tagMapper.selectList(
                new LambdaQueryWrapper<OshOpenProjectTag>()
                        .eq(OshOpenProjectTag::getDeleteFlag, (byte) 0)
                        .orderByAsc(OshOpenProjectTag::getSortOrder)
        );
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

    private String trimToMax(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
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
}

package com.backstage.system.controller.openproject;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.OshResourceId;
import com.backstage.common.annotation.OshUserEvent;
import com.backstage.common.annotation.OshUserLevel;
import com.backstage.common.constant.ResourceType;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.openproject.OshOpenProjectTag;
import com.backstage.system.domain.openproject.dto.OpenProjectEditDTO;
import com.backstage.system.domain.openproject.dto.OpenProjectLeaderTransferDTO;
import com.backstage.system.domain.openproject.dto.OpenProjectQueryDTO;
import com.backstage.system.domain.openproject.vo.OpenProjectRankVO;
import com.backstage.system.domain.openproject.vo.OpenProjectResourceOptionVO;
import com.backstage.system.domain.openproject.vo.OpenProjectVO;
import com.backstage.system.domain.vo.tool.ToolAnnouncementVO;
import com.backstage.system.mapper.openproject.OshOpenProjectAnnouncementMapper;
import com.backstage.system.mapper.openproject.OshOpenProjectResourceSearchMapper;
import com.backstage.system.service.openproject.IOshOpenProjectFavoriteService;
import com.backstage.system.service.openproject.IOshOpenProjectRankService;
import com.backstage.system.service.openproject.IOshOpenProjectService;
import com.backstage.system.utils.UserContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pc/openproject")
public class OshOpenProjectController {

    @Autowired
    private IOshOpenProjectService openProjectService;

    @Autowired
    private IOshOpenProjectFavoriteService favoriteService;

    @Autowired
    private IOshOpenProjectRankService rankService;

    @Autowired
    private OshOpenProjectAnnouncementMapper announcementMapper;

    @Autowired
    private OshOpenProjectResourceSearchMapper resourceSearchMapper;

    @GetMapping("/announcements")
    @Anonymous
    @OshUserEvent(module = "开源项目", actionType = "查询", resourceType = ResourceType.OPEN_PROJECT_TYPE, description = "查询开源项目公告", recordAnonymous = true)
    public R<List<ToolAnnouncementVO>> getAnnouncements() {
        List<ToolAnnouncementVO> announcements = new ArrayList<>();
        announcements.addAll(announcementMapper.selectLatestSyncedProjectAnnouncement());
        announcements.addAll(announcementMapper.selectLatestSourceAnnouncement());
        return R.ok(announcements);
    }

    @PostMapping("/list")
    @Anonymous
    @OshUserEvent(module = "开源项目", actionType = "查询", resourceType = ResourceType.OPEN_PROJECT_TYPE, description = "查询开源项目列表", recordAnonymous = true)
    public R<Map<String, Object>> list(@RequestBody(required = false) OpenProjectQueryDTO queryDTO) {
        return R.ok(openProjectService.listPage(queryDTO));
    }

    @PostMapping("/edit")
    @OshUserLevel(value = 4)
    @OshUserEvent(module = "开源项目", actionType = "编辑", resourceType = ResourceType.OPEN_PROJECT_TYPE, resourceNameExpression = "#p0.projectName", description = "编辑开源项目展示信息")
    public R<Void> edit(@RequestBody OpenProjectEditDTO dto) {
        try {
            openProjectService.updateProject(dto);
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/leader/transfer")
    @OshUserLevel(value = 4)
    @OshUserEvent(module = "开源项目", actionType = "编辑", resourceType = ResourceType.OPEN_PROJECT_TYPE, description = "转交开源项目最高负责人")
    public R<Void> transferLeader(@RequestBody OpenProjectLeaderTransferDTO dto) {
        try {
            openProjectService.transferLeader(dto);
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/tags")
    @Anonymous
    @OshUserEvent(module = "开源项目", actionType = "查询", resourceType = ResourceType.OPEN_PROJECT_TYPE, description = "查询开源项目标签", recordAnonymous = true)
    public R<List<OshOpenProjectTag>> tags() {
        return R.ok(openProjectService.listTags());
    }

    @GetMapping("/resource/search")
    @OshUserLevel(value = 4)
    @OshUserEvent(module = "开源项目", actionType = "查询", resourceType = ResourceType.OPEN_PROJECT_TYPE, description = "搜索可绑定资源")
    public R<List<OpenProjectResourceOptionVO>> searchResources(
            @RequestParam(defaultValue = "course") String resourceType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        if ("book".equalsIgnoreCase(resourceType)) {
            return R.ok(resourceSearchMapper.selectBooks(keyword, safeLimit));
        }
        if ("tool".equalsIgnoreCase(resourceType)) {
            return R.ok(resourceSearchMapper.selectTools(keyword, safeLimit));
        }
        return R.ok(resourceSearchMapper.selectCourses(keyword, safeLimit));
    }

    @GetMapping("/detail/{id}")
    @Anonymous
    @OshUserEvent(module = "开源项目", actionType = "浏览", resourceType = ResourceType.OPEN_PROJECT_TYPE, description = "浏览开源项目详情", recordAnonymous = true)
    public R<OpenProjectVO> detail(@OshResourceId @PathVariable Long id) {
        OpenProjectVO vo = openProjectService.getDetail(id);
        if (vo == null) {
            return R.fail("项目不存在");
        }
        return R.ok(vo);
    }

    @PutMapping("/click")
    @Anonymous
    @OshUserEvent(module = "开源项目", actionType = "点击", resourceType = ResourceType.OPEN_PROJECT_TYPE, description = "点击开源项目", recordAnonymous = true)
    public R<Void> click(@OshResourceId @RequestParam Long id) {
        try {
            openProjectService.incrementClickCount(id);
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/favorite")
    @OshUserLevel(value = 1)
    @OshUserEvent(module = "开源项目", actionType = "收藏", resourceType = ResourceType.OPEN_PROJECT_TYPE, description = "收藏开源项目")
    public R<Void> favorite(@OshResourceId @RequestParam Long projectId) {
        try {
            Long userId = UserContextUtil.getCurrentUserId();
            favoriteService.favorite(userId, projectId);
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/favorite/cancel")
    @OshUserLevel(value = 1)
    @OshUserEvent(module = "开源项目", actionType = "取消收藏", resourceType = ResourceType.OPEN_PROJECT_TYPE, description = "取消收藏开源项目")
    public R<Void> cancelFavorite(@OshResourceId @RequestParam Long projectId) {
        try {
            Long userId = UserContextUtil.getCurrentUserId();
            favoriteService.cancelFavorite(userId, projectId);
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/rank")
    @Anonymous
    @OshUserEvent(module = "开源项目", actionType = "查询", resourceType = ResourceType.OPEN_PROJECT_TYPE, description = "查询开源项目排行", recordAnonymous = true)
    public R<List<OpenProjectRankVO>> rank(
            @RequestParam(defaultValue = "star") String rankType,
            @RequestParam(defaultValue = "7") int period,
            @RequestParam(defaultValue = "10") int topN) {
        return R.ok(rankService.getRank(rankType, period, topN));
    }
}

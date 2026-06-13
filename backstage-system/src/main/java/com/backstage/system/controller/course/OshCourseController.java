package com.backstage.system.controller.course;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.DistributeLock;
import com.backstage.common.annotation.OshResourceId;
import com.backstage.common.annotation.OshUserEvent;
import com.backstage.common.annotation.OshUserLevel;
import com.backstage.common.constant.ResourceType;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;
import com.backstage.common.response.PageResponse;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.config.properties.SearchEsProperties;
import com.backstage.system.domain.course.OshCourse;
import com.backstage.system.domain.course.OshCourseMaterial;
import com.backstage.system.domain.course.vo.*;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.enums.behavior.ContributionResourceType;
import com.backstage.system.request.*;
import com.backstage.system.service.IOshCourseCollectionService;
import com.backstage.system.service.IOshCourseQuestionService;
import com.backstage.system.service.IOshCourseService;
import com.backstage.system.service.behavior.ContributionService;
import com.backstage.system.service.course.IOshCourseEsService;
import com.backstage.system.utils.UserContextUtil;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 课程信息 Controller
 *
 * @author ruoyi
 * @date 2026-01-XX
 */
@Api(tags = "课程管理")
@RestController
@RequestMapping("/pc/course")
public class OshCourseController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(OshCourseController.class);

    @Autowired
    private IOshCourseService oshCourseService;

    @Autowired
    private IOshCourseQuestionService oshCourseQuestionService;

    @Autowired
    private IOshCourseCollectionService oshCourseCollectionService;

    @Autowired
    private IOshCourseEsService oshCourseEsService;

    @Autowired
    private SearchEsProperties searchEsProperties;

    @Autowired
    private ContributionService contributionService;


    // TODO 后续追加 ES 查课
    // 免费,
    @ApiOperation("课程搜索")
    @PostMapping("/search")
    @OshUserEvent(module = "课程模块", actionType = "搜索", resourceType = ResourceType.COURSE_TYPE, description = "搜索课程", recordAnonymous = true)
//    @PreAuthorize("hasAuthority('course:list')")
    @Anonymous
    public R<PageResponse<CourseSearchLoginVo>> courseSearch(@RequestBody CourseSearchRequest request) {
        normalizeCollectionFilter(request);
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        applyCourseListSearchScope(request, currentOshUser);
        // 当用户请求查看"收藏"类型的课程（collectionFlag=1）但用户未登录时，直接返回一个空的分页结果，而不是继续执行搜索。
        if (Integer.valueOf(1).equals(request.getCollectionFlag()) && userId == null) {
            return R.ok(PageResponse.of(Collections.emptyList(), 0L, request.getPageNum(), request.getPageSize()), "ok");
        }
        // 管理员查看未发布课程时直接走 MySQL，避免 ES 索引延迟/缺失导致新建「审核中」课程不可见。
        boolean useEsSearch = searchEsProperties.isEnabled()
                && !Boolean.TRUE.equals(request.getIncludeUnpublished());
        if (useEsSearch) {
            try {
                log.info("使用es 查询");
                PageResponse<CourseSearchLoginVo> esResult = oshCourseEsService.searchCourses(request, userId);
                if (esResult != null && esResult.getTotal() > 0) {
                    return R.ok(esResult, "ok");
                }
                log.warn("course search es empty, fallback to mysql, request={}, userId={}", request, userId);
            } catch (Exception ex) {
                log.warn("course search fallback to mysql after es failure, request={}, userId={}", request, userId, ex);
            }
        }
        List<CourseSearchLoginVo> list = oshCourseService.pageQuerySearchCourse(userId, request);
        PageInfo<CourseSearchLoginVo> pageInfo = new PageInfo<>(list);
        return R.ok(PageResponse.of(pageInfo.getList(), pageInfo.getTotal(), pageInfo.getPageNum(), pageInfo.getPageSize()), "ok");
    }

    private void normalizeCollectionFilter(CourseSearchRequest request) {
        if (request == null) {
            return;
        }
        if (request.getCollectionFlag() == null && Boolean.TRUE.equals(request.getIsFollowing())) {
            request.setCollectionFlag(1);
        }
    }

    /** 创始人 role.level >= 6，可查看已隐藏（下架）课程 */
    private static final int FOUNDER_ROLE_LEVEL = 6;

    /**
     * 列表可见范围：
     * - 普通用户：仅已发布（status=4），看不到已隐藏（status=7）；
     * - 有课程管理权限者：可见未发布课程，但正常列表排除已隐藏（status=7）；
     * - 创始人（level>=6）点「已隐藏课程」：onlyHidden=true 时只返回已隐藏（status=7）。
     */
    private void applyCourseListSearchScope(CourseSearchRequest request, OshUser currentOshUser) {
        if (request == null) {
            return;
        }
        boolean canSeeUnpublished = canViewUnpublishedDetail(currentOshUser);
        boolean isFounder = UserContextUtil.getCurrentLevelSafely() != null
                && UserContextUtil.getCurrentLevelSafely() >= FOUNDER_ROLE_LEVEL;
        // 仅创始人可查看「已隐藏课程」分类，其余一律置空
        boolean onlyHidden = isFounder && Boolean.TRUE.equals(request.getOnlyHidden());
        request.setOnlyHidden(onlyHidden);
        // 查看已隐藏分类时需放开「仅已发布」限制，否则按权限决定
        request.setIncludeUnpublished(canSeeUnpublished || onlyHidden);
    }

    @ApiOperation("ES课程搜索")
    @PostMapping("/esSearch")
    @OshUserEvent(module = "课程模块", actionType = "搜索", resourceType = ResourceType.COURSE_TYPE, description = "ES搜索课程")
    @PreAuthorize("hasAuthority('course:list')")
    public R esCourseSearch(@RequestBody CourseSearchRequest request) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        Long userId = currentOshUser == null ? null : currentOshUser.getId();
        applyCourseListSearchScope(request, currentOshUser);
        return R.ok(oshCourseEsService.searchCourses(request, userId), "ok");
    }

    @ApiOperation("全量同步课程到ES")
    @PostMapping("/esSync/all")
    @Anonymous
    public R<Integer> syncAllCoursesToEs() {
//        return R.ok(oshCourseEsService.syncAllCoursesToEs(), "ok");
        return R.ok(oshCourseEsService.syncAllCoursesToEsWithoutStatusFilter(), "ok");
    }

//    @ApiOperation("全量同步课程到ES（不按状态过滤）")
//    @PostMapping("/esSync/all/raw")
//    @Anonymous
//    public R<Integer> syncAllCoursesToEsWithoutStatusFilter() {
//        return R.ok(oshCourseEsService.syncAllCoursesToEsWithoutStatusFilter(), "ok");
//    }


    @ApiOperation("登录态课程搜索")
    @PostMapping("/loginSearch/")
    @OshUserEvent(module = "课程模块", actionType = "搜索", resourceType = ResourceType.COURSE_TYPE, description = "登录态搜索课程", recordAnonymous = true)
    @Anonymous
    public R<PageResponse<CourseSearchLoginVo>> loginCourseSearch(@RequestBody CourseSearchRequest request) {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        if (userId == null) {
            return R.fail("请先登录");
        }
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        applyCourseListSearchScope(request, currentOshUser);
        return R.ok(oshCourseEsService.searchCourses(request, userId), "ok");
    }

    @ApiOperation("收藏课程搜索")
    @PostMapping("/search/collection")
    @OshUserEvent(module = "课程模块", actionType = "搜索", resourceType = ResourceType.COURSE_TYPE, description = "搜索收藏课程", recordAnonymous = true)
    @Anonymous
    public R<PageResponse<OshCourse>> collectionCourseSearch(@RequestBody CourseSearchRequest request) {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        if (userId == null) {
            return R.fail("请先登录");
        }
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        applyCourseListSearchScope(request, currentOshUser);
        List<OshCourse> list = oshCourseService.pageQueryUserCollectionCourse(userId, request);
        PageInfo<OshCourse> pageInfo = new PageInfo<>(list);
        return R.ok(PageResponse.of(pageInfo.getList(), pageInfo.getTotal(), pageInfo.getPageNum(), pageInfo.getPageSize()), "ok");
    }

    // TODO  后续需要 调用苍鳞方法去拿可访问性
    @ApiOperation("课程详情")
    @GetMapping("/detail/{id}")
    @OshUserEvent(module = "课程模块", actionType = "浏览", resourceType = ResourceType.COURSE_TYPE, description = "浏览课程详情", recordAnonymous = true)
    @Anonymous
    public R<OshCourseDetailVo> getCourseDetail(@OshResourceId @NotNull @PathVariable("id") Long id) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        boolean includeUnpublished = canViewUnpublishedDetail(currentOshUser);
        OshCourseDetailVo oshCourseDetailVo = oshCourseService.getCourseDetail(id, userId, includeUnpublished);
        if (oshCourseDetailVo == null) {
            return R.fail("课程不存在");
        }
        return R.ok(oshCourseDetailVo);
    }

    private boolean canViewUnpublishedDetail(OshUser currentOshUser) {
        if (currentOshUser == null) {
            return false;
        }
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return false;
            }
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            if (authorities == null || authorities.isEmpty()) {
                return false;
            }
            for (GrantedAuthority authority : authorities) {
                String perm = authority == null ? null : authority.getAuthority();
                if ("course:create".equals(perm)
                        || "course:update".equals(perm)
                        || "course:delete".equals(perm)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    @ApiOperation("获取小节内容 videoUrl or text")
    @GetMapping("/section/content/{courseId}/{sectionId}")
    @OshUserEvent(module = "课程模块", actionType = "学习", resourceType = ResourceType.COURSE_TYPE, description = "获取课程小节内容", recordAnonymous = true)
    @Anonymous
    public R<String> getCourseSectionContent(@OshResourceId @NotNull @PathVariable Long courseId, @NotNull @PathVariable Long sectionId) {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        if (!oshCourseService.canUserAccessSectionContent(courseId, sectionId, userId)) {
            return R.fail("您没有获取课程内容的权限，请购买后观看");
        }
        return R.ok(oshCourseService.getCourseSectionContent(sectionId, userId));
    }

    @ApiOperation("获取课程资料数组")
    @Anonymous
    @GetMapping("/section/materials/{courseId}")
    @OshUserEvent(module = "课程模块", actionType = "查询", resourceType = ResourceType.COURSE_TYPE, description = "查询课程资料", recordAnonymous = true)
    public R<List<OshCourseMaterial>> getCourseMaterials(@OshResourceId @NotNull @PathVariable Long courseId) {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        if (userId == null) {
            return R.fail("请先登录");
        }
        return R.ok(oshCourseService.getCourseMaterials(courseId));
    }



    @ApiOperation("课程章节内容")
    @GetMapping("/section/outline/{courseId}")
    @OshUserEvent(module = "课程模块", actionType = "查询", resourceType = ResourceType.COURSE_TYPE, description = "查询课程大纲", recordAnonymous = true)
    @Anonymous
    public R<List<OshCourseSectionVo>> getSectionOutline(@OshResourceId @NotNull @PathVariable Long courseId) {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        return R.ok(oshCourseService.getCourseSectionOutline(courseId, userId));
    }


    @ApiOperation("课程提问提交")
    @PostMapping("/section/submit")
    @OshUserEvent(module = "课程模块", actionType = "提交", resourceType = ResourceType.COURSE_TYPE, description = "提交课程提问")
    @PreAuthorize("hasAuthority('course:question:submit')")
    public R<Long> submitCourseSectionQuestion(@Validated @RequestBody CourseSectionQuestionRequest request) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        if (currentOshUser == null) {
            return R.fail("请先登录");
        }
        // 权限判断：level >= 2 直接放行；level < 2 时需要已付费该课程
        int userLevel = UserContextUtil.getCurrentLevel();
        if (userLevel < 2 && !oshCourseService.canUserAskQuestion(request.getCourseId(), request.getSectionId(), currentOshUser.getId())) {
            return R.fail("需要 VIP 及以上等级，或已购买该课程，才能提问");
        }
        return R.ok(oshCourseQuestionService.submitQuestion(currentOshUser.getId(), currentOshUser.getUsername(), request));
    }

    // TODO 需要校验只有购买了课程以及服务角色才能回答和追问, 部分免费的课程 没买课的也不能提问
    @ApiOperation("课程问题回答")
    @PostMapping("/question/answer")
    @OshUserEvent(module = "课程模块", actionType = "回答", resourceType = ResourceType.COURSE_TYPE, description = "回答课程问题")
    @PreAuthorize("hasAuthority('course:question:answer')")
    public R<Long> answerCourseQuestion(@Validated @RequestBody CourseQuestionAnswerRequest request) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        if (currentOshUser == null) {
            return R.fail("请先登录");
        }
        return R.ok(oshCourseQuestionService.answerQuestion(currentOshUser.getId(), currentOshUser.getUsername(), request));
    }

    @ApiOperation("新增/修改课程")
    @PostMapping("/save")
    @OshUserEvent(module = "课程模块", actionType = "新增", resourceType = ResourceType.COURSE_TYPE, resourceIdExpression = "#result.data", resourceNameExpression = "#p0.title", description = "新增或修改课程")
    @PreAuthorize("hasAuthority('course:create') or hasAuthority('course:update')")
    @DistributeLock(scene = "resource", key = "operation", expireTime = 10000, waitTime = 3000, releaseImmediately = true)
    public R<Long> save(@RequestBody CourseCreateRequest request) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        if (currentOshUser == null) return R.fail("请先登录");

        // 手动校验必填（新增时才强制校验关键字段，更新时可部分修改）
        if (request.getId() == null) {
            if (StringUtils.isBlank(request.getTitle())) return R.fail("课程标题不能为空");
            if (StringUtils.isBlank(request.getCover())) return R.fail("课程封面不能为空");
            if (StringUtils.isBlank(request.getIntro())) return R.fail("课程介绍不能为空");
            if (request.getPrice() == null) return R.fail("课程价格不能为空");
            if (request.getTPrice() == null) return R.fail("课程原价不能为空");
            if (StringUtils.isBlank(request.getType())) return R.fail("课程类型不能为空");
        } else {
            if (StringUtils.isBlank(request.getTitle())) return R.fail("课程标题不能为空");
        }

        Long courseId;
        if (request.getId() != null) {
            // 更新逻辑：将 CreateRequest 转换为 UpdateRequest
            CourseUpdateRequest updateRequest = buildUpdateRequest(request);
            courseId = oshCourseService.updateCourse(updateRequest, currentOshUser);
            if (courseId == null) return R.fail("修改课程失败");
        } else {
            // 新增逻辑
            courseId = oshCourseService.createCourse(request, currentOshUser);
            if (courseId == null) return R.fail("新增课程失败");
            contributionService.recordContribution(ContributionResourceType.COURSE.getCode(), courseId, request.getTitle());
        }
        return R.ok(courseId);
    }

    /**
     * 将 CourseCreateRequest 转换为 CourseUpdateRequest
     */
    private CourseUpdateRequest buildUpdateRequest(CourseCreateRequest req) {
        CourseUpdateRequest update = new CourseUpdateRequest();
        update.setId(req.getId());
        update.setTitle(req.getTitle());
        if (StringUtils.isNotBlank(req.getCover())) update.setCover(req.getCover());
        if (StringUtils.isNotBlank(req.getIntro())) update.setIntro(req.getIntro());
        update.setServiceContent(req.getServiceContent());
        update.setPrice(req.getPrice());
        update.setTPrice(req.getTPrice());
        update.setType(req.getType());
        update.setFreeType(req.getFreeType());
        update.setAfterServiceDays(req.getAfterServiceDays());
        update.setExamId(req.getExamId());
        update.setRemark(req.getRemark());
        update.setResourceType(req.getResourceType());
        update.setLevel(req.getLevel());
        update.setServicePeriod(req.getServicePeriod());
        update.setTags(req.getTags());
        update.setMaterial(req.getMaterial());
        return update;
    }


    // TODO 暂时只管控创建人可修改
    @ApiOperation("修改课程")
    @PostMapping("/update")
    @OshUserEvent(module = "课程模块", actionType = "修改", resourceType = ResourceType.COURSE_TYPE, resourceIdExpression = "#result.data", resourceNameExpression = "#p0.title", description = "修改课程")
    @PreAuthorize("hasAuthority('course:update')")
    @DistributeLock(scene = "resource", key = "operation", expireTime = 10000, waitTime = 3000, releaseImmediately = true)
    public R<Long> update(@Validated @RequestBody CourseUpdateRequest request) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        if (currentOshUser == null) {
            return R.fail("请先登录");
        }
        try {
            Long courseId = oshCourseService.updateCourse(request, currentOshUser);
            return R.ok(courseId);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    @ApiOperation("审核课程")
    @PostMapping("/audit")
    @OshUserEvent(module = "课程模块", actionType = "审核", resourceType = ResourceType.COURSE_TYPE, resourceIdExpression = "#p0.courseId", description = "审核课程")
    @PreAuthorize("hasAuthority('course:update')")
    @DistributeLock(scene = "course:audit", key = "api", expireTime = 60000, waitTime = 0, releaseImmediately = false)
    public R<Long> audit(@Validated @RequestBody CourseAuditRequest request) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        if (currentOshUser == null) {
            return R.fail("请先登录");
        }
        try {
            return R.ok(oshCourseService.auditCourse(request.getCourseId(), currentOshUser));
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    @ApiOperation("章节新增/修改")
    @PostMapping("/section/chapter/save")
    @OshUserEvent(module = "课程模块", actionType = "新增", resourceType = ResourceType.COURSE_TYPE, description = "新增或修改课程章节")
    @PreAuthorize("hasAuthority('course:chapter:save')")
    public R<Long> saveChapterSection(@Validated @RequestBody CourseChapterCreateRequest request) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        if (currentOshUser == null) {
            return R.fail("请先登录");
        }
        try {
            Long sectionId;
            if (request.getId() != null) {
                // 有 id → 更新章节标题/排序
                oshCourseService.updateCourseChapter(request, currentOshUser);
                sectionId = request.getId();
            } else {
                // 无 id → 新增章节（courseId 必填）
                if (request.getCourseId() == null) {
                    return R.fail("新增章节时课程ID不能为空");
                }
                sectionId = oshCourseService.createCourseChapter(request, currentOshUser);
            }
            return sectionId == null ? R.fail("操作失败") : R.ok(sectionId);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    @ApiOperation("章/节拖拽排序")
    @PostMapping("/section/reorder")
    @PreAuthorize("hasAuthority('course:chapter:save')")
    public R<Void> reorderSections(@Validated @RequestBody CourseSectionReorderRequest request) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        if (currentOshUser == null) {
            return R.fail("请先登录");
        }
        try {
            oshCourseService.reorderSections(request, currentOshUser);
            return R.ok();
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }


    @ApiOperation("视频小节添加")
    @PostMapping("/section/video/save")
    @OshUserEvent(module = "课程模块", actionType = "新增", resourceType = ResourceType.COURSE_TYPE, description = "新增课程视频小节")
    @PreAuthorize("hasAuthority('course:section:video')")
    public R<Long> saveVideoSection(@Validated @RequestBody CourseVideoSectionCreateRequest request) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        if (currentOshUser == null) {
            return R.fail("请先登录");
        }
        try {
            Long sectionId = oshCourseService.createCourseVideoSection(request, currentOshUser);
            return sectionId == null ? R.fail("新增视频小节失败") : R.ok(sectionId);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    @ApiOperation("文本内容小节添加")
    @PostMapping("/section/textContent/save")
    @OshUserEvent(module = "课程模块", actionType = "新增", resourceType = ResourceType.COURSE_TYPE, description = "新增课程文本小节")
    @PreAuthorize("hasAuthority('course:section:text')")
    public R<Long> saveTextSection(@Validated @RequestBody CourseTextSectionCreateRequest request) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        if (currentOshUser == null) {
            return R.fail("请先登录");
        }
        try {
            Long sectionId = oshCourseService.createCourseTextSection(request, currentOshUser);
            return sectionId == null ? R.fail("新增文本内容小节失败") : R.ok(sectionId);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    @ApiOperation("引入课程作为小节")
    @PostMapping("/section/courseLink/save")
    @PreAuthorize("hasAuthority('course:section:video')")
    public R<Long> saveCourseLinkSection(@Validated @RequestBody CourseLinkSectionCreateRequest request) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        if (currentOshUser == null) {
            return R.fail("请先登录");
        }
        try {
            Long sectionId = oshCourseService.createCourseLinkSection(request, currentOshUser);
            return sectionId == null ? R.fail("引入课程作为小节失败") : R.ok(sectionId);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }


    @ApiOperation("获取章节提问列表")
    @PostMapping("/section/questions/list")
    @OshUserEvent(module = "课程模块", actionType = "查询", resourceType = ResourceType.COURSE_TYPE, description = "查询章节提问", recordAnonymous = true)
    @Anonymous
    public R<List<CourseQuestionListItemVo>> getSectionQuestions(@Validated @RequestBody CourseSectionQuestionListRequest request) {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        return R.ok(oshCourseQuestionService.listSectionQuestions(userId, request), "ok");
    }

    // TODO 需要校验用户是否有权限对整个课程
    @ApiOperation("获取问题回答列表")
    @GetMapping("/question/answers/{questionId}")
    @OshUserEvent(module = "课程模块", actionType = "查询", resourceType = ResourceType.QA_QUESTION_TYPE, description = "查询课程问题回答", recordAnonymous = true)
    @Anonymous
    public R<List<CourseQuestionAnswerItemVo>> getQuestionAnswers(@OshResourceId @NotNull @PathVariable Long questionId) {
        return R.ok(oshCourseQuestionService.listQuestionAnswers(questionId));
    }

    @ApiOperation("收藏课程")
    @PostMapping("/collection/add")
    @OshUserEvent(module = "课程模块", actionType = "收藏", resourceType = ResourceType.COURSE_TYPE, description = "收藏课程")
    @Anonymous
//    @PreAuthorize("hasAuthority('course:collection:add')")
    public R collectCourse(@Validated @RequestBody CourseCollectionRequest request) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        if (currentOshUser == null) {
            return R.fail("请先登录");
        }
        oshCourseCollectionService.collectCourse(currentOshUser.getId(), currentOshUser.getUsername(), request.getCourseId());
        return R.ok("收藏课程成功");
    }

    @ApiOperation("取消收藏课程")
    @PostMapping("/collection/remove")
    @OshUserEvent(module = "课程模块", actionType = "取消收藏", resourceType = ResourceType.COURSE_TYPE, description = "取消收藏课程")
    @Anonymous
//    @PreAuthorize("hasAuthority('course:collection:remove')")
    public R removeCourseCollection(@Validated @RequestBody CourseCollectionRequest request) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        if (currentOshUser == null) {
            return R.fail("请先登录");
        }
        oshCourseCollectionService.removeCourseCollection(currentOshUser.getId(), currentOshUser.getUsername(), request.getCourseId());
        return R.ok("取消课程收藏成功");
    }

    @ApiOperation("删除章节/小节")
    @PostMapping("/sectionDelete")
    @OshUserEvent(module = "课程模块", actionType = "删除", resourceType = ResourceType.COURSE_TYPE, description = "删除课程章节")
    @PreAuthorize("hasAuthority('course:delete')")
    @Anonymous // 建议根据实际权限调整
    public R<String> deleteSection(@Validated @RequestBody CourseSectionDeleteRequest request) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        if (currentOshUser == null) return R.fail("请先登录");

        boolean success = oshCourseService.safeDeleteSection(request.getCourseId(), request.getSectionId(), currentOshUser);
        return success ? R.ok("删除成功") : R.fail("删除失败");
    }

    @ApiOperation("批量隐藏课程（下架，创始人专属）")
    @PostMapping("/hide")
    @OshUserEvent(module = "课程模块", actionType = "修改", resourceType = ResourceType.COURSE_TYPE, description = "隐藏课程")
    @OshUserLevel(value = 6)
    public R<String> hideCourses(@RequestBody CourseDeleteRequest request) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        if (currentOshUser == null) return R.fail("请先登录");
        if (request.getIds() == null || request.getIds().isEmpty()) return R.fail("请选择要隐藏的课程");
        try {
            oshCourseService.hideCoursesByIds(request.getIds(), currentOshUser);
            return R.ok("隐藏成功");
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    @ApiOperation("批量删除课程")
    @PostMapping("/delete")
    @OshUserEvent(module = "课程模块", actionType = "删除", resourceType = ResourceType.COURSE_TYPE, description = "删除课程")
    @PreAuthorize("hasAuthority('course:delete')")
    public R<String> deleteCourses(@RequestBody CourseDeleteRequest request) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        if (currentOshUser == null) return R.fail("请先登录");
        if (request.getIds() == null || request.getIds().isEmpty()) return R.fail("请选择要删除的课程");

        oshCourseService.deleteCoursesByIds(request.getIds(), currentOshUser);
        return R.ok("删除成功");
    }



}

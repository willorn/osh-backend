package com.backstage.system.service.impl;

import com.backstage.system.config.properties.SearchEsProperties;
import com.backstage.system.constants.CourseConstants;
import com.backstage.system.constants.CourseSectionConstants;
import com.backstage.system.domain.course.OshCourse;
import com.backstage.system.domain.course.OshCourseSection;
import com.backstage.system.domain.course.OshCourseMaterial;
import com.backstage.system.domain.course.OshCourseTag;
import com.backstage.system.domain.course.OshCourseTagRel;
import com.backstage.system.domain.document.OshDoc;
import com.backstage.system.domain.document.OshDocRef;
import com.backstage.system.domain.course.vo.CourseSearchLoginVo;
import com.backstage.system.domain.course.vo.OshCourseDetailVo;
import com.backstage.system.domain.course.vo.OshCourseSectionVo;
import com.backstage.system.domain.user.OshRole;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.enums.CourseResourceEnum;
import com.backstage.system.enums.OshCourseStatusEnum;
import com.backstage.system.mapper.course.OshCourseMapper;
import com.backstage.system.mapper.course.OshCourseCollectionMapper;
import com.backstage.system.mapper.course.OshCourseMaterialMapper;
import com.backstage.system.mapper.course.OshCourseSectionMapper;
import com.backstage.system.mapper.course.OshCourseTagMapper;
import com.backstage.system.mapper.document.OshDocMapper;
import com.backstage.system.mapper.user.OshRoleMapper;
import com.backstage.system.request.CourseCreateRequest;
import com.backstage.system.request.CourseChapterCreateRequest;
import com.backstage.system.request.CourseMaterialCreateRequest;
import com.backstage.system.request.CourseSearchRequest;
import com.backstage.system.request.CourseTextSectionCreateRequest;
import com.backstage.system.request.CourseUpdateRequest;
import com.backstage.system.request.CourseVideoSectionCreateRequest;
import com.backstage.system.service.IOshCourseService;
import com.backstage.system.service.OutboxEventService;
import com.backstage.system.service.common.OssService;
import com.backstage.system.service.course.CourseIndexDeleteMessage;
import com.backstage.system.service.course.CourseIndexEventType;
import com.backstage.system.service.course.CourseIndexMessageMapper;
import com.backstage.system.service.course.CourseIndexUpsertMessage;
import com.backstage.system.utils.UserContextUtil;
import com.backstage.system.service.course.ICourseManageService;
import com.backstage.system.service.course.IOshCourseEsService;
import com.backstage.system.utils.FileSizeConvertUtil;
import com.backstage.common.utils.generate.GenerateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.github.pagehelper.PageHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 课程信息 Service 业务层处理
 *
 * @author ruoyi
 * @date 2026-01-XX
 */
@Service
public class OshCourseServiceImpl implements IOshCourseService {
    @Autowired
    private OshCourseMapper oshCourseMapper;

    @Autowired
    private OshCourseCollectionMapper oshCourseCollectionMapper;

    @Autowired
    private OshCourseTagMapper oshCourseTagMapper;

    @Autowired
    private OshCourseMaterialMapper oshCourseMaterialMapper;

    @Autowired
    private ICourseManageService courseManageService;

    @Autowired
    private CourseIndexMessageMapper courseIndexMessageMapper;

    @Autowired
    private OutboxEventService outboxEventService;

    @Autowired
    private IOshCourseEsService oshCourseEsService;

    @Autowired
    private SearchEsProperties searchEsProperties;

    @Autowired
    private OshCourseSectionMapper oshCourseSectionMapper;

    @Autowired
    private OshRoleMapper oshRoleMapper;

    @Autowired
    private OshDocMapper oshDocMapper;

    /**
     * 对付费课程开放全部章节的角色（多角色时任一命中即可）。
     * 注意：developer 虽为 level 2，但属于普通开发者，不在此名单内。
     */
    private static final Set<String> FULL_ACCESS_ROLE_CODES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "vip", "small_class", "manager", "core_developer", "founder"
    )));

    /** 创始人 role.level >= 6，课程操作跳过审核直接发布 */
    private static final int FOUNDER_ROLE_LEVEL = 6;

    private static final String DOC_MODE_BIND_EXISTING = "BIND_EXISTING";
    private static final String DOC_REF_TYPE_COURSE_SECTION = "course_section";



    // 注入你之前提到的 OSS 服务接口
    @Autowired
    private OssService ossService;

    @Override
    public List<CourseSearchLoginVo> pageQuerySearchCourse(Long userId, CourseSearchRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<CourseSearchLoginVo> list;
        if (Integer.valueOf(1).equals(request.getCollectionFlag()) && userId != null) {
            list = oshCourseMapper.pageQueryUserCollectionSearchCourse(userId, request);
        } else {
             list = oshCourseMapper.pageQuerySearchCourse(request, userId);
            fillCollectionFlag(list, userId);
        }
        fillBuyFlag(list, userId);
        fillResourceTypeDesc(list, userId);
        return convertToExpiryUrls(list);
    }
    @Override
    public List<OshCourse> pageQueryUserCollectionCourse(Long userId, CourseSearchRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        return oshCourseMapper.pageQueryUserCollectionCourse(userId, request);
    }

    @Override
    public OshCourseDetailVo getCourseDetail(Long id, Long userId, boolean includeUnpublished) {
        // 1. 拿到详情 VO
        OshCourseDetailVo vo = oshCourseMapper.getCourseDetail(id, userId, includeUnpublished);
        if (vo == null) return null;
        fillResourceTypeDetail(vo);

        // 2. 封面处理（如果封面没写批量接口，就单调一次，或者你有 getCoverUrl 也可以换掉）
        if (StringUtils.isNotEmpty(vo.getCover())) {
            vo.setCover(ossService.getLimitedUrl(vo.getCover(), 1440));
        }

        // 3. 先计算 accessLevel，再按权限签发/屏蔽小节媒体地址
        vo.setAccessLevel(resolveAccessLevel(vo, userId));
        applySectionMediaByAccess(vo.getSections(), vo.getAccessLevel());

        return vo;
    }

    /**
     * 判断用户对该课程的访问级别
     * FULL  - 全部章节可看
     * TRIAL - 仅免费试看章节
     */
    private String resolveAccessLevel(OshCourseDetailVo vo, Long userId) {
        // 1. 免费课程，所有人全开放
        Integer freeType = vo.getFreeType();
        if (freeType != null && (freeType == 0 || freeType == 2)) {
            return "FULL";
        }

        // 2. 未登录，只能试看
        if (userId == null) {
            return "TRIAL";
        }

        // 3. VIP 及以上角色直接全开放（developer 除外）
        if (hasFullCourseAccessByRole(userId)) {
            return "FULL";
        }

        // 4. 普通用户：查是否已单独购买该课程
        if (oshCourseMapper.countUserBoughtCourse(vo.getId(), userId) > 0) {
            return "FULL";
        }

        // 5. 其他情况：仅试看
        return "TRIAL";
    }

    /**
     * 遍历用户全部有效角色，任一 VIP 及以上角色即可全量访问课程。
     */
    private boolean hasFullCourseAccessByRole(Long userId) {
        if (userId == null) {
            return false;
        }
        List<Integer> roleIds = oshRoleMapper.getRoleIdsByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<OshRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(OshRole::getId, roleIds)
                .eq(OshRole::getDeleteFlag, 0)
                .eq(OshRole::getStatus, 1);
        List<OshRole> roles = oshRoleMapper.selectList(roleWrapper);
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        for (OshRole role : roles) {
            String roleCode = role.getRoleCode();
            if (roleCode != null && FULL_ACCESS_ROLE_CODES.contains(roleCode.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Integer isUserBuyCourseOrFreeCourse(Long courseId, Long userId) {
        return oshCourseMapper.isUserBuyCourseOrFreeCourse(courseId, userId);
    }

    @Override
    public boolean hasUserBoughtCourse(Long courseId, Long userId) {
        return oshCourseMapper.countUserBoughtCourse(courseId, userId) > 0;
    }

    @Override
    public boolean canUserAskQuestion(Long courseId, Long sectionId, Long userId) {
        if (courseId == null) {
            return false;
        }
        if (userId != null && oshCourseMapper.countUserBoughtCourse(courseId, userId) > 0) {
            return true;
        }
        if (oshCourseMapper.countFreeCourse(courseId) > 0) {
            return true;
        }
        return sectionId != null && oshCourseMapper.countFreeSectionInCourse(courseId, sectionId) > 0;
    }

    @Override
    public String getCourseSectionContent(Long sectionId, Long userId) {
        OshCourseSection section = oshCourseMapper.selectCourseSectionById(sectionId);
        if (section == null || section.getDeleteFlag() == null || section.getDeleteFlag() != CourseSectionConstants.DELETE_FLAG_NORMAL) {
            return "";
        }
        if (CourseSectionConstants.TYPE_TEXT.equalsIgnoreCase(section.getType())) {
            String content = oshDocMapper.selectPrimaryDocContentBySectionId(sectionId);
            return StringUtils.defaultString(content, "");
        }
        if (CourseSectionConstants.TYPE_VIDEO.equalsIgnoreCase(section.getType())) {
            return StringUtils.defaultString(section.getMediaUrl(), "");
        }
        return "";
    }

    @Override
    public List<OshCourseMaterial> getCourseMaterials(Long courseId) {
        List<OshCourseMaterial> courseMaterials = oshCourseMapper.getCourseMaterials(courseId);
        if (courseMaterials == null || courseMaterials.isEmpty()) return courseMaterials;

        // 批量生成临时URL回填
        List<Long> ids = courseMaterials.stream()
                .map(OshCourseMaterial::getId)
                .collect(Collectors.toList());
        Map<Long, String> urlMap = courseManageService.batchGetMaterialUrls(ids, 120);

        courseMaterials.forEach(m -> {
            String signedUrl = urlMap.get(m.getId());
            if (StringUtils.isNotEmpty(signedUrl)) {
                m.setUrl(signedUrl);
            }
        });
        return courseMaterials;
    }



    @Override
    public List<OshCourseSectionVo> getCourseSectionOutline(Long courseId, Long userId) {
        List<OshCourseSectionVo> sectionTree = buildSectionTree(oshCourseMapper.selectCourseSectionList(courseId));
        if (sectionTree == null || sectionTree.isEmpty()) {
            return sectionTree;
        }
        String accessLevel = resolveAccessLevel(buildAccessVo(courseId), userId);
        applySectionMediaByAccess(sectionTree, accessLevel);
        return sectionTree;
    }

    @Override
    public boolean canUserAccessSectionContent(Long courseId, Long sectionId, Long userId) {
        if (courseId == null || sectionId == null) {
            return false;
        }
        OshCourse course = oshCourseMapper.selectCourseById(courseId);
        if (course == null) {
            return false;
        }
        OshCourseDetailVo accessVo = new OshCourseDetailVo();
        accessVo.setId(courseId);
        accessVo.setFreeType(course.getFreeType());
        if ("FULL".equals(resolveAccessLevel(accessVo, userId))) {
            return true;
        }
        return oshCourseMapper.countFreeSectionInCourse(courseId, sectionId) > 0;
    }

    private OshCourseDetailVo buildAccessVo(Long courseId) {
        OshCourseDetailVo vo = new OshCourseDetailVo();
        vo.setId(courseId);
        OshCourse course = oshCourseMapper.selectCourseById(courseId);
        if (course != null) {
            vo.setFreeType(course.getFreeType());
        }
        return vo;
    }

    private void applySectionMediaByAccess(List<OshCourseSectionVo> sections, String accessLevel) {
        if (sections == null || sections.isEmpty()) {
            return;
        }
        boolean fullAccess = "FULL".equals(accessLevel);
        List<Long> accessibleIds = new ArrayList<>();
        collectAccessibleSectionIds(sections, fullAccess, accessibleIds);
        Map<Long, String> videoUrlMap = accessibleIds.isEmpty()
                ? Collections.emptyMap()
                : courseManageService.batchGetSectionVideoUrls(accessibleIds, 360);
        applyUrlsWithAccess(sections, videoUrlMap, fullAccess);
    }

    private void collectAccessibleSectionIds(List<OshCourseSectionVo> nodes, boolean fullAccess, List<Long> ids) {
        for (OshCourseSectionVo node : nodes) {
            if (node == null) {
                continue;
            }
            if (fullAccess || isFreePreviewSection(node)) {
                ids.add(node.getId());
            }
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                collectAccessibleSectionIds(node.getChildren(), fullAccess, ids);
            }
        }
    }

    private void applyUrlsWithAccess(List<OshCourseSectionVo> nodes, Map<Long, String> videoUrlMap, boolean fullAccess) {
        for (OshCourseSectionVo node : nodes) {
            if (node == null) {
                continue;
            }
            boolean canAccess = fullAccess || isFreePreviewSection(node);
            if (canAccess && videoUrlMap.containsKey(node.getId())) {
                node.setMediaUrl(videoUrlMap.get(node.getId()));
            } else if (!canAccess) {
                node.setMediaUrl(null);
                node.setTextContent(null);
            }
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                applyUrlsWithAccess(node.getChildren(), videoUrlMap, fullAccess);
            }
        }
    }

    private boolean isFreePreviewSection(OshCourseSectionVo section) {
        return section != null && section.getFreeFlag() != null && section.getFreeFlag() == 1;
    }

    /**
     * 递归收集 ID
     */
    private void collectIds(List<OshCourseSectionVo> nodes, List<Long> allIds) {
        for (OshCourseSectionVo node : nodes) {
            allIds.add(node.getId());
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                collectIds(node.getChildren(), allIds);
            }
        }
    }

    /**
     * 递归回填 URL
     */
    private void applyUrls(List<OshCourseSectionVo> nodes, Map<Long, String> videoUrlMap) {
        for (OshCourseSectionVo node : nodes) {
            if (videoUrlMap.containsKey(node.getId())) {
                node.setMediaUrl(videoUrlMap.get(node.getId()));
            }
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                applyUrls(node.getChildren(), videoUrlMap);
            }
        }
    }

    /**
     * 查询课程详情
     *
     * @param id 课程 ID
     * @return 课程信息
     */
    @Override
    public OshCourse selectCourseById(Long id) {
        return oshCourseMapper.selectCourseById(id);
    }

    /**
     * 查询课程列表
     *
     * @return 课程集合
     */
    @Override
    public List<OshCourse> selectCourseList() {
        return oshCourseMapper.selectCourseList();
    }

    /**
     * 新增课程
     *
     * @param course 课程信息
     * @return 结果
     */
    @Override
    public int insertCourse(OshCourse course) {
        return oshCourseMapper.insertCourse(course);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCourse(CourseCreateRequest request, OshUser operator) {
        OshCourse course = buildCourseForCreate(request, operator);
        course.setStatus(resolveCourseStatusAfterOperatorAction(operator));
        int rows = oshCourseMapper.insertCourse(course);
        if (rows <= 0) {
            return null;
        }
        bindCourseMaterial(course.getId(), request.getMaterial(), operator);
        bindCourseTags(course.getId(), request.getTags(), operator);
        OshCourse latestCourse = ensureCourseExists(course.getId());
        CourseIndexUpsertMessage indexMessage = buildCourseIndexUpsertMessage(
                latestCourse, request.getTags(), operator, CourseIndexEventType.COURSE_INDEX_CREATE);
        publishCourseIndexOutboxIfNeeded(course.getId(), latestCourse.getStatus(), indexMessage, operator);
        scheduleCourseEsUpsertAfterCommit(course.getId(), latestCourse.getStatus());
        return course.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long updateCourse(CourseUpdateRequest request, OshUser operator) {
        ensureCourseExists(request.getId());
        OshCourse course = buildCourseForUpdate(request, operator);
        course.setStatus(resolveCourseStatusAfterOperatorAction(operator));
        int rows = oshCourseMapper.updateCourse(course);
        if (rows <= 0) {
            return null;
        }
        if (request.getMaterial() != null) {
            rebuildCourseMaterial(course.getId(), request.getMaterial(), operator);
        }
        if (request.getTags() != null) {
            rebuildCourseTags(course.getId(), request.getTags(), operator);
        }
        OshCourse latestCourse = ensureCourseExists(course.getId());
        CourseIndexUpsertMessage indexMessage = buildCourseIndexUpsertMessage(
                latestCourse, request.getTags(), operator, CourseIndexEventType.COURSE_INDEX_UPDATE);
        publishCourseIndexOutboxIfNeeded(course.getId(), latestCourse.getStatus(), indexMessage, operator);
        scheduleCourseEsUpsertAfterCommit(course.getId(), latestCourse.getStatus());
        return course.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long auditCourse(Long courseId, OshUser operator) {
        OshCourse existingCourse = ensureCourseExists(courseId);
        if (!Objects.equals(existingCourse.getStatus(), OshCourseStatusEnum.PENDING_AUDIT.getCode())) {
            throw new IllegalArgumentException("只有待审核课程才可以审核通过");
        }

        OshCourse course = new OshCourse();
        course.setId(courseId);
        // New/edited courses must go through the audit queue first.
        course.setStatus(OshCourseStatusEnum.PENDING_AUDIT.getCode());
        course.setUpdateBy(operator == null ? null : StringUtils.trimToNull(operator.getUsername()));
        int rows = oshCourseMapper.updateCourse(course);
        if (rows <= 0) {
            throw new IllegalArgumentException("审核课程失败");
        }
        OshCourse latestCourse = ensureCourseExists(courseId);
        CourseIndexUpsertMessage indexMessage = buildCourseIndexUpsertMessage(
                latestCourse, null, operator, CourseIndexEventType.COURSE_INDEX_UPDATE);
        publishCourseIndexOutboxIfNeeded(courseId, latestCourse.getStatus(), indexMessage, operator);
        scheduleCourseEsUpsertAfterCommit(courseId, latestCourse.getStatus());
        return courseId;
    }

    @Override
    public void updateCourseChapter(CourseChapterCreateRequest request, OshUser operator) {
        OshCourseSection section = new OshCourseSection();
        Date now = new Date();
        section.setId(request.getId());
        section.setTitle(request.getTitle());
        section.setSort(request.getSort());
        section.setUpdateBy(String.valueOf(operator.getId()));
        section.setUpdateTime(now);
        oshCourseMapper.updateCourseSection(section);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void hideCoursesByIds(List<Long> ids, OshUser operator) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        String operatorName = operator == null ? null : StringUtils.trimToNull(operator.getUsername());
        Integer hiddenStatus = OshCourseStatusEnum.HIDDEN.getCode();

        for (Long courseId : ids) {
            OshCourse existingCourse = ensureCourseExists(courseId);
            if (existingCourse.getDeleteFlag() != null && existingCourse.getDeleteFlag() != 0) {
                throw new IllegalArgumentException("课程不存在");
            }
            if (Objects.equals(existingCourse.getStatus(), hiddenStatus)) {
                continue;
            }

            OshCourse course = new OshCourse();
            course.setId(courseId);
            course.setStatus(hiddenStatus);
            course.setUpdateBy(operatorName);
            int rows = oshCourseMapper.updateCourse(course);
            if (rows <= 0) {
                throw new IllegalArgumentException("隐藏课程失败");
            }

            OshCourse latestCourse = ensureCourseExists(courseId);
            CourseIndexUpsertMessage indexMessage = buildCourseIndexUpsertMessage(
                    latestCourse, null, operator, CourseIndexEventType.COURSE_INDEX_UPDATE);
            publishCourseIndexOutboxIfNeeded(courseId, latestCourse.getStatus(), indexMessage, operator);
            scheduleCourseEsUpsertAfterCommit(courseId, latestCourse.getStatus());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCoursesByIds(List<Long> ids, OshUser operator) {
        if (ids == null || ids.isEmpty()) return;
        String operatorName = operator == null ? null : operator.getUsername();

        for (Long courseId : ids) {
            ensureCourseExists(courseId);

            // 1. 软删除课程主表
            oshCourseMapper.deleteCourseById(courseId);

            // 2. 软删除所有章节和小节（delete_flag=1）
            oshCourseMapper.deleteSectionsByCourseId(courseId, operatorName);

            // 3. 删除标签关联
            oshCourseTagMapper.deleteCourseTagRelationByCourseId(courseId);

            // 4. 删除资料
            oshCourseMaterialMapper.deleteMaterialsByCourseId(courseId);

            // 5. 记录删除 outbox 事件，交给定时任务异步投递到 Kafka，再由 Flink 删除 ES 文档
            CourseIndexDeleteMessage deleteMessage = new CourseIndexDeleteMessage(courseId);
            deleteMessage.setEventType(CourseIndexEventType.COURSE_INDEX_DELETE);
            outboxEventService.saveCourseIndexDeleteEvent(courseId, deleteMessage, operator);
        }
    }



    @Override
    public Long createCourseChapter(CourseChapterCreateRequest request, OshUser operator) {
        ensureCourseExists(request.getCourseId());
        OshCourseSection section = buildChapterSectionForCreate(request, operator);
        return insertCourseSection(section);
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCourseTextSection(CourseTextSectionCreateRequest request, OshUser operator) {
        validateTextDocPayload(request);

        // 有 id 时走更新；无 id 时走新增
        if (request.getId() != null) {
            OshCourseSection existingSection = ensureSectionExists(request.getId());
            ensureCourseExists(existingSection.getCourseId());
            ensureParentChapter(existingSection.getCourseId(), request.getParentId());

            OshCourseSection section = buildTextSectionForUpdate(request, existingSection, operator);
            oshCourseMapper.updateCourseSection(section);
            saveOrBindSectionDoc(section.getId(), request, operator);
            return section.getId();
        }

        ensureCourseExists(request.getCourseId());
        ensureParentChapter(request.getCourseId(), request.getParentId());
        OshCourseSection section = buildTextSectionForCreate(request, operator);
        Long sectionId = insertCourseSection(section);
        if (sectionId == null) {
            throw new IllegalArgumentException("新增文本小节失败");
        }
        saveOrBindSectionDoc(sectionId, request, operator);
        return sectionId;
    }

    @Override
    public Long createCourseVideoSection(CourseVideoSectionCreateRequest request, OshUser operator) {
        // 有 id → 更新
        if (request.getId() != null) {
            OshCourseSection section = buildVideoSectionForUpdate(request, operator);
            section.setId(request.getId());
            oshCourseMapper.updateCourseSection(section);
            if (shouldHandleDocForVideoRequest(request)) {
                saveOrBindSectionDoc(request.getId(), buildDocBindRequestForVideo(request), operator);
            }
            return request.getId();
        }
        // 无 id → 新增，先校验必填
        if (StringUtils.isBlank(request.getMediaUrl())) {
            throw new IllegalArgumentException("视频地址不能为空");
        }
        ensureCourseExists(request.getCourseId());
        ensureParentChapter(request.getCourseId(), request.getParentId());
        OshCourseSection section = buildVideoSectionForCreate(request, operator);
        Long sectionId = insertCourseSection(section);
        if (sectionId != null && shouldHandleDocForVideoRequest(request)) {
            saveOrBindSectionDoc(sectionId, buildDocBindRequestForVideo(request), operator);
        }
        return sectionId;
    }



    /**
     * 修改课程
     *
     * @param course 课程信息
     * @return 结果
     */
    @Override
    public int updateCourse(OshCourse course) {
        return oshCourseMapper.updateCourse(course);
    }

    /**
     * 批量删除课程
     *
     * @param ids 需要删除的课程 ID
     * @return 结果
     */
    @Override
    public int deleteCourseByIds(Long[] ids) {
        return oshCourseMapper.deleteCourseByIds(ids);
    }

    /**
     * 删除课程
     *
     * @param id 课程 ID
     * @return 结果
     */
    @Override
    public int deleteCourseById(Long id) {
        return oshCourseMapper.deleteCourseById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean safeDeleteSection(Long courseId, Long sectionId, OshUser operator) {
        // 1. 获取要删除的节点
        OshCourseSection section = oshCourseMapper.selectCourseSectionById(sectionId);

        // 2. 基础安全性校验：是否存在、是否已删除、是否属于当前课程
        if (section == null || section.getDeleteFlag().intValue() != CourseSectionConstants.DELETE_FLAG_NORMAL) {
            return false;
        }
        if (courseId != null && !courseId.equals(section.getCourseId())) {
            return false;
        }

        String operatorName = operator == null ? null : StringUtils.trimToNull(operator.getUsername());

        // 3. 逻辑判断：如果是父章节，需要联通下面的子小节一起删除
        if (section.getParentId() == null || CourseSectionConstants.ROOT_PARENT_ID.equals(section.getParentId())) {
            // 是章节：调用按 parent_id 批量删除子小节
            oshCourseMapper.deleteCourseSectionsByParentId(sectionId, operatorName);
        }

        // 4. 删除当前节点本身
        return oshCourseMapper.deleteCourseSectionById(sectionId, operatorName) > 0;
    }


    private OshCourseSection buildVideoSectionForUpdate(CourseVideoSectionCreateRequest req, OshUser operator) {
        OshCourseSection s = new OshCourseSection();
        s.setTitle(req.getTitle());
        s.setFreeFlag(req.getFreeFlag());
        s.setDuration(req.getDuration());
        if (req.getMediaUrl() != null && !req.getMediaUrl().isEmpty()) {
            s.setMediaUrl(req.getMediaUrl()); // 只有传了才更新
        }
        s.setTextContent(req.getTextContent());
        s.setFileSize(req.getFileSize());
        s.setSort(req.getSort());
        s.setUpdateBy(operator.getUsername());
        return s;
    }


    /**
     * 内部私有方法：循环处理 List 中的封面 URL
     */
    private List<CourseSearchLoginVo> convertToExpiryUrls(List<CourseSearchLoginVo> list) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }

        // 提取所有课程ID
        List<Long> courseIds = list.stream().map(CourseSearchLoginVo::getId).collect(Collectors.toList());

        // 调用你之前写好的那个批量获取封面 URL 的业务逻辑 (有效期设为 60 分钟)
        // 假设这个方法在 courseManageService 中
        Map<Long, String> urlMap = courseManageService.batchGetCourseCoverUrls(courseIds, 60);

        // 将生成的签名 URL 塞回 List 里的对象
        for (CourseSearchLoginVo vo : list) {
            String signedUrl = urlMap.get(vo.getId());
            if (StringUtils.isNotEmpty(signedUrl)) {
                vo.setCover(signedUrl); // 这里直接覆盖掉原来的相对路径
            }
        }
        return list;
    }

    static List<OshCourseSectionVo> buildSectionTree(List<OshCourseSectionVo> sectionList) {
        if (sectionList == null || sectionList.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, OshCourseSectionVo> nodeMap = new HashMap<>();
        for (OshCourseSectionVo section : sectionList) {
            section.setChildren(new ArrayList<>());
            nodeMap.put(section.getId(), section);
        }

        List<OshCourseSectionVo> roots = new ArrayList<>();
        for (OshCourseSectionVo section : sectionList) {
            Long parentId = section.getParentId();
            if (parentId == null || parentId == 0L) {
                roots.add(section);
                continue;
            }
            OshCourseSectionVo parent = nodeMap.get(parentId);
            if (parent == null) {
                roots.add(section);
                continue;
            }
            parent.getChildren().add(section);
        }

        sortSections(roots);
        return roots;
    }

    private static void sortSections(List<OshCourseSectionVo> sections) {
        sections.sort(Comparator
                .comparing(OshCourseSectionVo::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(OshCourseSectionVo::getId, Comparator.nullsLast(Long::compareTo)));
        for (OshCourseSectionVo section : sections) {
            if (section.getChildren() != null && !section.getChildren().isEmpty()) {
                sortSections(section.getChildren());
            }
        }
    }

    static OshCourse buildCourseForCreate(CourseCreateRequest request, OshUser operator) {
        OshCourse course = new OshCourse();
        course.setTitle(StringUtils.trimToNull(request.getTitle()));
        course.setCover(StringUtils.trimToNull(request.getCover()));
        course.setIntro(StringUtils.trimToNull(request.getIntro()));
        course.setServiceContent(StringUtils.trimToNull(request.getServiceContent()));
        course.setPrice(request.getPrice());
        course.setTPrice(request.getTPrice());
        course.setType(StringUtils.trimToNull(request.getType()));
        // freeType 由前端根据 resourceType 传入，不用 defaultInteger 兜底为 0（0=完全免费会导致付费课被误判）
        course.setFreeType(request.getFreeType() != null ? request.getFreeType() : 3);
        course.setAfterServiceDays(defaultInteger(request.getAfterServiceDays()));
        course.setExamId(request.getExamId());
        course.setRemark(StringUtils.trimToNull(request.getRemark()));
        course.setResourceType(request.getResourceType());
        course.setLevel(request.getLevel());
        course.setServicePeriod(request.getServicePeriod());

        course.setSubCount(CourseConstants.DEFAULT_COUNT);
        course.setTotalDuration(CourseConstants.DEFAULT_COUNT);
        course.setVideoCount(CourseConstants.DEFAULT_COUNT);
        course.setSalesCount(CourseConstants.DEFAULT_COUNT);
        course.setViewCount(CourseConstants.DEFAULT_LONG_COUNT);
        course.setFreeLessonCount(CourseConstants.DEFAULT_COUNT);
        course.setLikeCount(CourseConstants.DEFAULT_COUNT);
        course.setCommentCount(CourseConstants.DEFAULT_COUNT);
        course.setRatingScore(CourseConstants.DEFAULT_RATING_SCORE);

        String operatorName = operator == null ? null : StringUtils.trimToNull(operator.getUsername());
        course.setCreateBy(operatorName);
        course.setUpdateBy(operatorName);
        return course;
    }

    private static Integer defaultInteger(Integer value) {
        return value == null ? CourseConstants.DEFAULT_COUNT : value;
    }

    /**
     * 创始人创建/编辑课程直接发布（status=4 审核通过），其余角色进入待审核（status=2）。
     * 优先查库中有效角色，避免 ThreadLocal level 未刷新时误判。
     */
    private Integer resolveCourseStatusAfterOperatorAction(OshUser operator) {
        if (isFounderOperator(operator)) {
            return OshCourseStatusEnum.PUBLISHED.getCode();
        }
        return OshCourseStatusEnum.PENDING_AUDIT.getCode();
    }

    private boolean isFounderOperator(OshUser operator) {
        if (UserContextUtil.getCurrentLevelSafely() >= FOUNDER_ROLE_LEVEL) {
            return true;
        }
        if (operator == null || operator.getId() == null) {
            return false;
        }
        List<Integer> roleIds = oshRoleMapper.getRoleIdsByUserId(operator.getId());
        if (roleIds == null || roleIds.isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<OshRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(OshRole::getId, roleIds)
                .eq(OshRole::getDeleteFlag, 0)
                .eq(OshRole::getStatus, 1);
        List<OshRole> roles = oshRoleMapper.selectList(roleWrapper);
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        for (OshRole role : roles) {
            String roleCode = role.getRoleCode();
            if (roleCode != null && "founder".equals(roleCode.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    static OshCourse buildCourseForUpdate(CourseUpdateRequest request, OshUser operator) {
        OshCourse course = new OshCourse();
        course.setId(request.getId());
        course.setTitle(StringUtils.trimToNull(request.getTitle()));
        course.setCover(StringUtils.trimToNull(request.getCover()));
        course.setIntro(StringUtils.trimToNull(request.getIntro()));
        course.setServiceContent(StringUtils.trimToNull(request.getServiceContent()));
        course.setPrice(request.getPrice());
        course.setTPrice(request.getTPrice());
        course.setType(StringUtils.trimToNull(request.getType()));
        course.setFreeType(request.getFreeType() != null ? request.getFreeType() : 3);
        course.setAfterServiceDays(defaultInteger(request.getAfterServiceDays()));
        course.setExamId(request.getExamId());
        course.setRemark(StringUtils.trimToNull(request.getRemark()));
        course.setResourceType(request.getResourceType());
        course.setLevel(request.getLevel());
        if (request.getServicePeriod() != null) course.setServicePeriod(request.getServicePeriod());
        String operatorName = operator == null ? null : StringUtils.trimToNull(operator.getUsername());
        course.setUpdateBy(operatorName);
        return course;
    }

    private void bindCourseMaterial(Long courseId, CourseMaterialCreateRequest materialRequest, OshUser operator) {
        if (materialRequest == null) {
            return;
        }
        if (materialRequest.getMaterialId() != null) {
            return;
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(materialRequest.getFileName())) {
            throw new com.backstage.common.exception.ServiceException("资料文件名称不能为空");
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(materialRequest.getFileUrl())) {
            throw new com.backstage.common.exception.ServiceException("资料文件地址不能为空");
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(materialRequest.getFileType())) {
            throw new com.backstage.common.exception.ServiceException("资料文件类型不能为空");
        }
        if (materialRequest.getFileSize() == null || materialRequest.getFileSize().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new com.backstage.common.exception.ServiceException("资料文件大小不能为空");
        }
        OshCourseMaterial material = buildCourseMaterialForCreate(courseId, materialRequest, operator);
        oshCourseMaterialMapper.insertMaterialEntity(material);
    }

    static String normalizeMaterialRelativePath(String fileUrl) {
        if (StringUtils.isBlank(fileUrl) || "keep".equalsIgnoreCase(fileUrl.trim())) {
            return null;
        }
        String trimmed = fileUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            throw new com.backstage.common.exception.ServiceException("资料地址必须为OSS相对路径，请重新上传资料");
        }
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
    }

    static OshCourseMaterial buildCourseMaterialForCreate(Long courseId, CourseMaterialCreateRequest materialRequest, OshUser operator) {
        String fileName = StringUtils.trimToNull(materialRequest.getFileName());
        String fileUrl = normalizeMaterialRelativePath(materialRequest.getFileUrl());
        if (StringUtils.isBlank(fileUrl)) {
            throw new com.backstage.common.exception.ServiceException("资料文件地址不能为空");
        }
        String fileType = StringUtils.trimToNull(materialRequest.getFileType());

        OshCourseMaterial material = new OshCourseMaterial();
        material.setCourseId(courseId);
        material.setMaterialName(fileName);
        material.setFileUrl(fileUrl);
        material.setFileType(fileType);
        material.setFileSize(FileSizeConvertUtil.convertBytesToKb(materialRequest.getFileSize()));
        material.setSort(CourseConstants.DEFAULT_COUNT);
        material.setDeleteFlag(CourseConstants.DEFAULT_COUNT);

        String operatorName = operator == null ? null : StringUtils.trimToNull(operator.getUsername());
        material.setCreateBy(operatorName);
        material.setUpdateBy(operatorName);
        return material;
    }

    private void rebuildCourseMaterial(Long courseId, CourseMaterialCreateRequest materialRequest, OshUser operator) {
        // 如果传了 materialId，说明是已有资料，直接保留，不删不增
        if (materialRequest != null && materialRequest.getMaterialId() != null) {
            return;
        }
        oshCourseMaterialMapper.deleteMaterialsByCourseId(courseId);
        bindCourseMaterial(courseId, materialRequest, operator);
    }

    /**
     * 填充资源类型描述,
     * @param list
     * @return
     */
    static List<CourseSearchLoginVo> fillResourceTypeDesc(List<CourseSearchLoginVo> list, Long userId) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        for (CourseSearchLoginVo item : list) {
            if (item == null) {
                continue;
            }
            boolean needPurchasedDesc = CourseResourceEnum.CASH_ONLY.getCode().equals(item.getResourceType())
                    || CourseResourceEnum.CASH_POINT.getCode().equals(item.getResourceType());
            if (userId != null && needPurchasedDesc && Integer.valueOf(1).equals(item.getBuyFlag())) {
                item.setResourceTypeDesc("已购买");
                continue;
            }
            CourseResourceEnum resourceEnum = CourseResourceEnum.fromCode(item.getResourceType());
            item.setResourceTypeDesc(resourceEnum == null ? item.getResourceType() : resourceEnum.getDesc());
        }
        return list;
    }

    private void fillBuyFlag(List<CourseSearchLoginVo> list, Long userId) {
        if (userId == null || list == null || list.isEmpty()) {
            return;
        }
        List<Long> courseIds = list.stream().map(CourseSearchLoginVo::getId).collect(Collectors.toList());
        if (courseIds.isEmpty()) {
            return;
        }
        List<Long> boughtCourseIds = oshCourseMapper.selectUserBoughtCourseIds(userId, courseIds);
        if (boughtCourseIds == null || boughtCourseIds.isEmpty()) {
            return;
        }
        Set<Long> boughtCourseIdSet = new HashSet<>(boughtCourseIds);
        for (CourseSearchLoginVo item : list) {
            if (boughtCourseIdSet.contains(item.getId())) {
                item.setBuyFlag(1);
            }
        }
    }

    private void fillCollectionFlag(List<CourseSearchLoginVo> list, Long userId) {
        if (userId == null || list == null || list.isEmpty()) {
            return;
        }
        List<Long> courseIds = list.stream().map(CourseSearchLoginVo::getId).collect(Collectors.toList());
        if (courseIds.isEmpty()) {
            return;
        }
        List<Long> collectedCourseIds = oshCourseCollectionMapper.selectActiveCourseIdsByUserIdAndCourseIds(userId, courseIds);
        if (collectedCourseIds == null || collectedCourseIds.isEmpty()) {
            return;
        }
        Set<Long> collectedCourseIdSet = new HashSet<>(collectedCourseIds);
        for (CourseSearchLoginVo item : list) {
            if (collectedCourseIdSet.contains(item.getId())) {
                item.setCollectionFlag(1);
            }
        }
    }

    private void fillResourceTypeDetail(OshCourseDetailVo vo) {
        if (vo == null) {
            return;
        }
        CourseResourceEnum resourceEnum = CourseResourceEnum.fromCode(vo.getResourceType());
        if (resourceEnum != null) {
            // 只设置描述字段，保留原始 code 供前端回显使用
            vo.setResourceTypeDesc(resourceEnum.getDesc());
        }
    }

    private void bindCourseTags(Long courseId, List<String> tags, OshUser operator) {
        List<String> normalizedTags = normalizeCourseTags(tags);
        if (normalizedTags.isEmpty()) {
            return;
        }
        for (String tagName : normalizedTags) {
            OshCourseTag tag = resolveCourseTag(tagName, operator);
            insertCourseTagRelation(courseId, tag.getId(), operator);
            oshCourseTagMapper.increaseUseCount(tag.getId());
        }
    }

    private void rebuildCourseTags(Long courseId, List<String> tags, OshUser operator) {
        oshCourseTagMapper.deleteCourseTagRelationByCourseId(courseId);
        bindCourseTags(courseId, tags, operator);
    }

    static List<String> normalizeCourseTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, String> tagMap = new LinkedHashMap<>();
        for (String tag : tags) {
            String normalizedName = StringUtils.trimToNull(tag);
            if (normalizedName == null) {
                continue;
            }
            tagMap.putIfAbsent(normalizedName, normalizedName);
        }
        return new ArrayList<>(tagMap.values());
    }

    // 在创建课程时，如果标签不存在，则创建
    private OshCourseTag resolveCourseTag(String tagName, OshUser operator) {
        OshCourseTag existing = oshCourseTagMapper.selectCourseTagByName(tagName);
        if (existing != null) {
            return existing;
        }

        OshCourseTag tag = buildCourseTagForCreate(tagName, operator);
        try {
            oshCourseTagMapper.insertCourseTag(tag);
            return tag;
        } catch (DuplicateKeyException ex) {
            OshCourseTag retry = oshCourseTagMapper.selectCourseTagByName(tagName);
            if (retry != null) {
                return retry;
            }
            throw ex;
        }
    }

    private void insertCourseTagRelation(Long courseId, Long tagId, OshUser operator) {
        OshCourseTagRel relation = new OshCourseTagRel();
        Date now = new Date();
        relation.setCourseId(courseId);
        relation.setTagId(tagId);
        relation.setDeleteFlag(CourseConstants.DEFAULT_COUNT);
        String operatorName = operator == null ? null : StringUtils.trimToNull(operator.getUsername());
        relation.setCreateBy(operatorName);
        relation.setCreateTime(now);
        relation.setUpdateBy(operatorName);
        relation.setUpdateTime(now);
        oshCourseTagMapper.insertCourseTagRel(relation);
    }

    static OshCourseTag buildCourseTagForCreate(String tagName, OshUser operator) {
        OshCourseTag tag = new OshCourseTag();
        Date now = new Date();
        tag.setName(StringUtils.trimToNull(tagName));
        tag.setSort(CourseConstants.DEFAULT_COUNT);
        tag.setUseCount(CourseConstants.DEFAULT_COUNT);
        tag.setStatus(CourseSectionConstants.STATUS_NORMAL);
        tag.setDeleteFlag(CourseConstants.DEFAULT_COUNT);
        String operatorName = operator == null ? null : StringUtils.trimToNull(operator.getUsername());
        tag.setCreateBy(operatorName);
        tag.setCreateTime(now);
        tag.setUpdateBy(operatorName);
        tag.setUpdateTime(now);
        return tag;
    }

    static OshCourseSection buildChapterSectionForCreate(CourseChapterCreateRequest request, OshUser operator) {
        OshCourseSection section = buildBaseSection(request.getCourseId(), CourseSectionConstants.ROOT_PARENT_ID,
                request.getTitle(), request.getSort(), operator);
        section.setFreeFlag(CourseSectionConstants.CHAPTER_FREE_FLAG);
        return section;
    }

    static OshCourseSection buildVideoSectionForCreate(CourseVideoSectionCreateRequest request, OshUser operator) {
        OshCourseSection section = buildBaseSection(request.getCourseId(), request.getParentId(),
                request.getTitle(), request.getSort(), operator);
        section.setFreeFlag(defaultFreeFlag(request.getFreeFlag()));
        section.setDuration(request.getDuration());
        section.setMediaUrl(StringUtils.trimToNull(request.getMediaUrl()));
        section.setCover(StringUtils.trimToNull(request.getCover()));
        section.setVideoDesc(StringUtils.trimToNull(request.getVideoDesc()));
        section.setTextContent(StringUtils.trimToNull(request.getTextContent()));
        section.setFileSize(request.getFileSize());
        section.setType(CourseSectionConstants.TYPE_VIDEO);
        return section;
    }

    static OshCourseSection buildTextSectionForCreate(CourseTextSectionCreateRequest request, OshUser operator) {
        OshCourseSection section = buildBaseSection(request.getCourseId(), request.getParentId(),
                request.getTitle(), request.getSort(), operator);
        section.setFreeFlag(defaultFreeFlag(request.getFreeFlag()));
        section.setTextContent(StringUtils.trimToNull(request.getTextContent()));
        section.setType(CourseSectionConstants.TYPE_TEXT);
        return section;
    }

    static OshCourseSection buildTextSectionForUpdate(CourseTextSectionCreateRequest request, OshCourseSection existing, OshUser operator) {
        OshCourseSection section = new OshCourseSection();
        section.setId(existing.getId());
        section.setTitle(StringUtils.trimToNull(request.getTitle()));
        section.setSort(request.getSort());
        section.setFreeFlag(defaultFreeFlag(request.getFreeFlag()));
        section.setTextContent(StringUtils.trimToNull(request.getTextContent()));
        String operatorName = operator == null ? null : StringUtils.trimToNull(operator.getUsername());
        section.setUpdateBy(operatorName);
        section.setUpdateTime(new Date());
        return section;
    }

    private Long insertCourseSection(OshCourseSection section) {
        int rows = oshCourseMapper.insertCourseSection(section);
        return rows > 0 ? section.getId() : null;
    }

    private void validateTextDocPayload(CourseTextSectionCreateRequest request) {
        boolean bindExisting = isBindExistingDocMode(request);
        if (bindExisting && request.getDocId() == null) {
            throw new IllegalArgumentException("绑定已有文档时 docId 不能为空");
        }
        if (!bindExisting && StringUtils.isBlank(request.getTextContent())) {
            throw new IllegalArgumentException("文本内容不能为空");
        }
    }

    private boolean isBindExistingDocMode(CourseTextSectionCreateRequest request) {
        if (request.getDocId() != null) {
            return true;
        }
        return StringUtils.equalsIgnoreCase(DOC_MODE_BIND_EXISTING, request.getDocMode());
    }

    private boolean shouldHandleDocForVideoRequest(CourseVideoSectionCreateRequest request) {
        if (request == null) {
            return false;
        }
        if (request.getDocId() != null) {
            return true;
        }
        if (StringUtils.isNotBlank(request.getDocMode())
                && !StringUtils.equalsIgnoreCase(DOC_MODE_BIND_EXISTING, request.getDocMode())) {
            return true;
        }
        if (StringUtils.equalsIgnoreCase(DOC_MODE_BIND_EXISTING, request.getDocMode())) {
            return true;
        }
        return StringUtils.isNotBlank(request.getTextContent());
    }

    private CourseTextSectionCreateRequest buildDocBindRequestForVideo(CourseVideoSectionCreateRequest request) {
        CourseTextSectionCreateRequest bindRequest = new CourseTextSectionCreateRequest();
        bindRequest.setId(request.getId());
        bindRequest.setCourseId(request.getCourseId());
        bindRequest.setParentId(request.getParentId());
        bindRequest.setTitle(request.getTitle());
        bindRequest.setSort(request.getSort());
        bindRequest.setFreeFlag(request.getFreeFlag());
        bindRequest.setTextContent(request.getTextContent());
        bindRequest.setDocMode(request.getDocMode());
        bindRequest.setDocId(request.getDocId());
        bindRequest.setDocTitle(request.getDocTitle());
        bindRequest.setAnchorType(request.getAnchorType());
        bindRequest.setAnchorStart(request.getAnchorStart());
        bindRequest.setAnchorEnd(request.getAnchorEnd());
        bindRequest.setExcerptTitle(request.getExcerptTitle());
        return bindRequest;
    }

    private void saveOrBindSectionDoc(Long sectionId, CourseTextSectionCreateRequest request, OshUser operator) {
        String operatorName = operator == null ? null : StringUtils.trimToNull(operator.getUsername());
        Date now = new Date();
        Long currentDocId = oshDocMapper.selectPrimaryDocIdBySectionId(sectionId);
        boolean bindExisting = isBindExistingDocMode(request);
        boolean hasTextUpdate = StringUtils.isNotBlank(request.getTextContent()) || StringUtils.isNotBlank(request.getDocTitle());
        String docTitle = StringUtils.defaultIfBlank(request.getDocTitle(), request.getTitle());
        Long docId;

        if (bindExisting) {
            if (request.getDocId() == null) {
                throw new IllegalArgumentException("绑定已有文档时 docId 不能为空");
            }
            OshDoc existingDoc = oshDocMapper.selectDocById(request.getDocId());
            if (existingDoc == null) {
                throw new IllegalArgumentException("要绑定的文档不存在");
            }
            docId = existingDoc.getId();
            ensureSectionDocRef(sectionId, docId, request, operatorName, now);
            return;
        }

        if (currentDocId != null) {
            docId = currentDocId;
            if (hasTextUpdate) {
                OshDoc updateDoc = new OshDoc();
                updateDoc.setId(currentDocId);
                updateDoc.setTitle(docTitle);
                updateDoc.setContent(StringUtils.defaultString(request.getTextContent(), ""));
                updateDoc.setContentFormat("html");
                updateDoc.setUpdateBy(operatorName);
                updateDoc.setUpdateTime(now);
                oshDocMapper.updateDocContent(updateDoc);
            }
        } else {
            OshDoc newDoc = new OshDoc();
            newDoc.setId(GenerateUtil.generateSnowflakeId());
            newDoc.setTitle(StringUtils.defaultIfBlank(docTitle, "Section#" + sectionId));
            newDoc.setContentFormat("html");
            newDoc.setContent(StringUtils.defaultString(request.getTextContent(), ""));
            newDoc.setSummary(null);
            newDoc.setStatus(OshCourseStatusEnum.PUBLISHED.getCode());
            newDoc.setVisibility(1);
            newDoc.setDeleteFlag(0);
            newDoc.setCreateBy(operatorName);
            newDoc.setCreateTime(now);
            newDoc.setUpdateBy(operatorName);
            newDoc.setUpdateTime(now);
            oshDocMapper.insertDoc(newDoc);
            docId = newDoc.getId();
        }

        ensureSectionDocRef(sectionId, docId, request, operatorName, now);
    }

    private void ensureSectionDocRef(Long sectionId,
                                     Long docId,
                                     CourseTextSectionCreateRequest request,
                                     String operatorName,
                                     Date now) {
        OshDocRef activeRef = oshDocMapper.selectActivePrimaryDocRefBySectionId(sectionId);
        String anchorType = StringUtils.defaultIfBlank(request.getAnchorType(), "full");
        String anchorStart = StringUtils.defaultString(request.getAnchorStart(), "");
        String anchorEnd = StringUtils.defaultString(request.getAnchorEnd(), "");
        String excerptTitle = StringUtils.trimToNull(request.getExcerptTitle());

        if (activeRef != null
                && Objects.equals(activeRef.getDocId(), docId)
                && StringUtils.equals(StringUtils.defaultString(activeRef.getAnchorType(), "full"), anchorType)
                && StringUtils.equals(StringUtils.defaultString(activeRef.getAnchorStart(), ""), anchorStart)
                && StringUtils.equals(StringUtils.defaultString(activeRef.getAnchorEnd(), ""), anchorEnd)) {
            return;
        }

        oshDocMapper.purgeSoftDeletedSectionDocRefs(sectionId);
        if (activeRef != null) {
            oshDocMapper.softDeleteSectionDocRefs(sectionId, operatorName);
        }

        OshDocRef ref = new OshDocRef();
        ref.setId(GenerateUtil.generateSnowflakeId());
        ref.setDocId(docId);
        ref.setRefType(DOC_REF_TYPE_COURSE_SECTION);
        ref.setRefId(sectionId);
        ref.setIsPrimary(1);
        ref.setSort(0);
        ref.setAnchorType(anchorType);
        ref.setAnchorStart(anchorStart);
        ref.setAnchorEnd(anchorEnd);
        ref.setExcerptTitle(excerptTitle);
        ref.setDeleteFlag(0);
        ref.setCreateBy(operatorName);
        ref.setCreateTime(now);
        ref.setUpdateBy(operatorName);
        ref.setUpdateTime(now);
        oshDocMapper.insertDocRef(ref);
    }

    private OshCourse ensureCourseExists(Long courseId) {
        OshCourse course = oshCourseMapper.selectCourseById(courseId);
        if (course == null) {
            throw new IllegalArgumentException("课程不存在");
        }
        return course;
    }

    private OshCourseSection ensureSectionExists(Long sectionId) {
        OshCourseSection section = oshCourseMapper.selectCourseSectionById(sectionId);
        if (section == null || section.getDeleteFlag() == null || section.getDeleteFlag() != CourseSectionConstants.DELETE_FLAG_NORMAL) {
            throw new IllegalArgumentException("小节不存在");
        }
        return section;
    }

    private void ensureParentChapter(Long courseId, Long parentId) {
        OshCourseSection parent = oshCourseMapper.selectCourseSectionById(parentId);
        if (parent == null || parent.getDeleteFlag() == null || parent.getDeleteFlag() != CourseSectionConstants.DELETE_FLAG_NORMAL) {
            throw new IllegalArgumentException("父章节不存在");
        }
        if (!courseId.equals(parent.getCourseId())) {
            throw new IllegalArgumentException("父章节不属于当前课程");
        }
        if (parent.getParentId() == null || !CourseSectionConstants.ROOT_PARENT_ID.equals(parent.getParentId())) {
            throw new IllegalArgumentException("父章节必须为一级章节");
        }
    }

    private static OshCourseSection buildBaseSection(Long courseId, Long parentId, String title, Integer sort, OshUser operator) {
        OshCourseSection section = new OshCourseSection();
        Date now = new Date();
        section.setCourseId(courseId);
        section.setParentId(parentId);
        section.setTitle(StringUtils.trimToNull(title));
        section.setSort(sort);
        section.setStatus(CourseSectionConstants.STATUS_NORMAL);
        section.setDeleteFlag(CourseSectionConstants.DELETE_FLAG_NORMAL);
        String operatorName = operator == null ? null : StringUtils.trimToNull(operator.getUsername());
        section.setCreateBy(operatorName);
        section.setCreateTime(now);
        section.setUpdateBy(operatorName);
        section.setUpdateTime(now);
        return section;
    }

    private static Integer defaultFreeFlag(Integer freeFlag) {
        return freeFlag == null ? CourseSectionConstants.DEFAULT_FREE_FLAG : freeFlag;
    }

    /**
     * 待审核/已下架课程不走 Outbox upsert：旧版 Flink 会把非已发布文档从 ES 删除，
     * 导致「手动 esSync 有数据、新建后审核页看不到」的问题。这类状态改由事务提交后直接 upsert。
     */
    private void publishCourseIndexOutboxIfNeeded(Long courseId, Integer status,
                                                  CourseIndexUpsertMessage message, OshUser operator) {
        if (!OshCourseStatusEnum.PUBLISHED.getCode().equals(status)) {
            return;
        }
        outboxEventService.saveCourseIndexEvent(courseId, message, operator);
    }

    private void scheduleCourseEsUpsertAfterCommit(Long courseId, Integer status) {
        if (!searchEsProperties.isEnabled() || courseId == null || !shouldDirectSyncCourseToEs(status)) {
            return;
        }
        Runnable upsertTask = () -> oshCourseEsService.upsertCourseById(courseId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    upsertTask.run();
                }
            });
        } else {
            upsertTask.run();
        }
    }

    private boolean shouldDirectSyncCourseToEs(Integer status) {
        return OshCourseStatusEnum.PENDING_AUDIT.getCode().equals(status)
                || OshCourseStatusEnum.PUBLISHED.getCode().equals(status)
                || OshCourseStatusEnum.OFF_SHELF.getCode().equals(status)
                // 隐藏课程同步写入 ES（status=7），普通用户的 ES 查询按 status=4 过滤后自然不可见
                || OshCourseStatusEnum.HIDDEN.getCode().equals(status);
    }

    private CourseIndexUpsertMessage buildCourseIndexUpsertMessage(OshCourse course, List<String> tags, OshUser operator, String eventType) {
        Date now = new Date();
        String operatorName = operator == null ? null : StringUtils.trimToNull(operator.getUsername());
        CourseIndexUpsertMessage message = courseIndexMessageMapper.toMessage(course, operatorName);
        if (message == null) {
            message = new CourseIndexUpsertMessage();
        }
        message.setEventType(eventType);
        message.setCollectionCount(CourseConstants.DEFAULT_COUNT);
        message.setDeleteFlag(CourseConstants.DEFAULT_COUNT);
        if (message.getCreateTime() == null) {
            message.setCreateTime(now);
        }
        message.setUpdateTime(now);

        List<String> tagNames = extractTagNames(tags);
        message.setTagNames(tagNames);
        String tagNamesText = String.join(" ", tagNames);
        message.setTagNamesText(tagNamesText);
        message.setSearchText(buildCourseSearchText(course, tagNamesText));
        return message;
    }

    private List<String> extractTagNames(List<String> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return Collections.emptyList();
        }
        return normalizeCourseTags(tags);
    }

    private String buildCourseSearchText(OshCourse course, String tagNamesText) {
        StringBuilder builder = new StringBuilder();
        appendSearchField(builder, course.getTitle());
        appendSearchField(builder, course.getIntro());
        appendSearchField(builder, course.getServiceContent());
        appendSearchField(builder, tagNamesText);
        return builder.toString().trim();
    }

    private void appendSearchField(StringBuilder builder, String value) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(value.trim());
    }


}

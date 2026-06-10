package com.backstage.system.controller.course;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.Log;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;
import com.backstage.common.enums.BusinessType;
import com.backstage.system.domain.course.OshCourse;
import com.backstage.system.domain.dto.*;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.vo.*;
import com.backstage.system.service.course.ICourseManageService;
import com.backstage.system.utils.UserContextUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.backstage.common.utils.StringUtils;

/**
 * 课程管理 Controller
 * 
 * @author ruoyi
 * @date 2026-03-24
 */
@Api(tags = "课程管理")
@RestController
@RequestMapping("/pc/course")
public class CourseManageController extends BaseController {
    
    @Autowired
    private ICourseManageService courseManageService;
    
    // ==================== 课程封面URL批量获取接口 ====================
    
    /**
     * 批量获取课程封面临时访问URL（根据课程ID）
     * 方案B：课程列表返回相对路径，前端按需调用此接口获取临时URL
     * 适用场景：课程列表页、推荐位等需要批量展示封面的场景
     * 
     * @param courseIds 课程ID列表（最多50个），格式：1,2,3,4,5
     * @param minute 临时URL有效期（分钟），默认30分钟
     * @return 课程ID到临时URL的映射
     */
    @Anonymous
    @ApiOperation("批量获取课程封面临时URL（按课程ID）")
    @GetMapping("/covers")
    public R<Map<Long, String>> batchGetCoverUrls(
            @ApiParam("课程ID列表，最多50个") @RequestParam("courseIds") String courseIds,
            @ApiParam("URL有效期（分钟），默认30") @RequestParam(value = "minute", required = false, defaultValue = "30") Integer minute) {
        // 解析课程ID列表
        List<Long> idList = new ArrayList<>();
        if (StringUtils.isNotEmpty(courseIds)) {
            for (String id : courseIds.split(",")) {
                try {
                    idList.add(Long.parseLong(id.trim()));
                } catch (NumberFormatException ignored) {
                    // 忽略无效的ID
                }
            }
        }
        
        if (idList.isEmpty()) {
            return R.ok(new HashMap<>(), "ok");
        }
        
        // 限制最多50个
        if (idList.size() > 50) {
            idList = idList.subList(0, 50);
        }
        
        // 限制minute不超过1440分钟（24小时）
        int validMinute = Math.min(minute != null ? minute : 30, 1440);
        
        Map<Long, String> result = courseManageService.batchGetCourseCoverUrls(idList, validMinute);
        return R.ok(result, "ok");
    }
    
    /**
     * 批量获取课程封面临时访问URL（根据相对路径）
     * 适用场景：前端已有封面相对路径，直接转换获取临时URL
     * 
     * @param paths 封面相对路径列表（最多50个），格式：路径1,路径2,路径3
     * @param minute 临时URL有效期（分钟），默认30分钟
     * @return 相对路径到临时URL的映射
     */
    @Anonymous
    @ApiOperation("批量获取课程封面临时URL（按相对路径）")
    @GetMapping("/cover/urls")
    public R<Map<String, String>> batchGetCoverUrlsByPaths(
            @ApiParam("封面相对路径列表，最多50个") @RequestParam("paths") String paths,
            @ApiParam("URL有效期（分钟），默认30") @RequestParam(value = "minute", required = false, defaultValue = "30") Integer minute) {
        // 解析路径列表
        List<String> pathList = new ArrayList<>();
        if (StringUtils.isNotEmpty(paths)) {
            for (String path : paths.split(",")) {
                if (StringUtils.isNotEmpty(path.trim())) {
                    pathList.add(path.trim());
                }
            }
        }
        
        if (pathList.isEmpty()) {
            return R.ok(new HashMap<>(), "ok");
        }
        
        // 限制最多50个
        if (pathList.size() > 50) {
            pathList = pathList.subList(0, 50);
        }
        
        // 限制minute不超过1440分钟（24小时）
        int validMinute = Math.min(minute != null ? minute : 30, 1440);
        
        Map<String, String> result = courseManageService.batchGetCoverUrlsByPaths(pathList, validMinute);
        return R.ok(result, "ok");
    }
    
    // ==================== 章节视频临时URL接口 ====================
    
    /**
     * 批量获取章节视频临时访问URL
     * 方案B：章节详情返回相对路径，前端按需调用此接口获取临时URL
     * 适用场景：视频播放器加载、批量预加载视频URL等
     * 
     * @param sectionIds 章节ID列表（最多50个），格式：1,2,3,4,5
     * @param minute 临时URL有效期（分钟），默认60分钟
     * @return 章节ID到临时URL的映射
     */
    @Anonymous
    @ApiOperation("批量获取章节视频临时URL")
    @GetMapping("/section/video-urls")
    public R<Map<Long, String>> batchGetSectionVideoUrls(
            @ApiParam("章节ID列表，最多50个") @RequestParam("sectionIds") String sectionIds,
            @ApiParam("URL有效期（分钟），默认60") @RequestParam(value = "minute", required = false, defaultValue = "60") Integer minute) {
        // 解析章节ID列表
        List<Long> idList = new ArrayList<>();
        if (StringUtils.isNotEmpty(sectionIds)) {
            for (String id : sectionIds.split(",")) {
                try {
                    idList.add(Long.parseLong(id.trim()));
                } catch (NumberFormatException ignored) {
                    // 忽略无效的ID
                }
            }
        }
        
        if (idList.isEmpty()) {
            return R.ok(new HashMap<>(), "ok");
        }
        
        // 限制最多50个
        if (idList.size() > 50) {
            idList = idList.subList(0, 50);
        }
        
        // 限制minute不超过360分钟
        int validMinute = Math.min(minute != null ? minute : 60, 360);
        
        Map<Long, String> result = courseManageService.batchGetSectionVideoUrls(idList, validMinute);
        return R.ok(result, "ok");
    }
    
    /**
     * 单个获取章节视频临时访问URL
     * 适用场景：单个视频播放加载
     * 
     * @param sectionId 章节ID
     * @param minute 临时URL有效期（分钟），默认60分钟
     * @return 临时访问URL
     */
    @Anonymous
    @ApiOperation("获取章节视频临时URL")
    @GetMapping("/section/video-url")
    public R<String> getSectionVideoUrl(
            @ApiParam("章节ID") @RequestParam("sectionId") Long sectionId,
            @ApiParam("URL有效期（分钟），默认60") @RequestParam(value = "minute", required = false, defaultValue = "60") Integer minute) {
        if (sectionId == null) {
            return R.ok("", "ok");
        }
        
        // 限制minute不超过360分钟
        int validMinute = Math.min(minute != null ? minute : 60, 360);
        
        String result = courseManageService.getSectionVideoUrl(sectionId, validMinute);
        return R.ok(result != null ? result : "", "ok");
    }
    
    // ==================== 课程资料临时URL接口 ====================
    
    /**
     * 批量获取课程资料临时访问URL
     * 方案B：资料列表返回相对路径，前端按需调用此接口获取临时URL
     * 适用场景：资料下载、批量预加载资料URL等
     * 
     * @param materialIds 资料ID列表（最多50个），格式：1,2,3,4,5
     * @param minute 临时URL有效期（分钟），默认120分钟
     * @return 资料ID到临时URL的映射
     */
    @Anonymous
    @ApiOperation("批量获取课程资料临时URL")
    @GetMapping("/material-urls")
    public R<Map<Long, String>> batchGetMaterialUrls(
            @ApiParam("资料ID列表，最多50个") @RequestParam("materialIds") String materialIds,
            @ApiParam("URL有效期（分钟），默认120") @RequestParam(value = "minute", required = false, defaultValue = "120") Integer minute) {
        // 解析资料ID列表
        List<Long> idList = new ArrayList<>();
        if (StringUtils.isNotEmpty(materialIds)) {
            for (String id : materialIds.split(",")) {
                try {
                    idList.add(Long.parseLong(id.trim()));
                } catch (NumberFormatException ignored) {
                    // 忽略无效的ID
                }
            }
        }
        
        if (idList.isEmpty()) {
            return R.ok(new HashMap<>(), "ok");
        }
        
        // 限制最多50个
        if (idList.size() > 50) {
            idList = idList.subList(0, 50);
        }
        
        // 限制minute不超过360分钟
        int validMinute = Math.min(minute != null ? minute : 120, 360);
        
        Map<Long, String> result = courseManageService.batchGetMaterialUrls(idList, validMinute);
        return R.ok(result, "ok");
    }
    
    /**
     * 单个获取课程资料临时访问URL
     * 适用场景：单个资料下载
     * 
     * @param materialId 资料ID
     * @param minute 临时URL有效期（分钟），默认120分钟
     * @return 临时访问URL
     */
    @Anonymous
    @ApiOperation("获取课程资料临时URL")
    @GetMapping("/material-url")
    public R<String> getMaterialUrl(
            @ApiParam("资料ID") @RequestParam("materialId") Long materialId,
            @ApiParam("URL有效期（分钟），默认120") @RequestParam(value = "minute", required = false, defaultValue = "120") Integer minute) {
        if (materialId == null) {
            return R.ok("", "ok");
        }
        
        // 限制minute不超过360分钟
        int validMinute = Math.min(minute != null ? minute : 120, 360);
        
        String result = courseManageService.getMaterialUrl(materialId, validMinute);
        return R.ok(result != null ? result : "", "ok");
    }
    
    // ==================== 课程列表查询接口 ====================

    @Anonymous
    @ApiOperation("获取全部标签&模糊查询标签")
    @GetMapping("/tags")
    public R<List<Map<String, Object>>> getTags(@ApiParam("关键字")  @RequestParam(required = false) String keyword) {
        return R.ok(courseManageService.searchTags(keyword));
    }

    /**
     * 分页查询课程列表
     */
    @Anonymous
    @ApiOperation("查询课程列表")
    @GetMapping("/list")
    public R<?> list(CourseQueryDTO courseDTO) {
        return R.ok(courseManageService.selectCourseList(courseDTO));
    }
    
    /**
     * 获取课程详情
     */
    @Anonymous
    @ApiOperation("获取课程详情")
    @GetMapping("/{courseId}")
    public R<CourseDetailVO> getCourseDetail(
            @ApiParam("课程 ID") @PathVariable Long courseId) {
        Long userId = getUserId();
        return R.ok(courseManageService.getCourseDetail(courseId, userId));
    }
    /**
     * 上传视频：返回视频信息
     */
    @Log(title = "视频上传", businessType = BusinessType.INSERT)
    @Anonymous
    @ApiOperation("上传视频")
    @PostMapping("/video/upload")
    public R<Map<String, Object>> uploadVideo(
            @ApiParam("视频文件") @RequestParam("file") MultipartFile file,
            @ApiParam("视频名称") @RequestParam(value = "videoName", required = false) String videoName,
            @ApiParam("小节 ID（重新上传时传，用于删除旧视频）") @RequestParam(value = "sectionId", required = false) Long sectionId) {
        Map<String, Object> videoInfo = courseManageService.uploadVideo(file, videoName, sectionId);
        return R.ok(videoInfo);
    }



    /**
     * 上传课程资料：返回资料信息
     */
    @Log(title = "课程资料", businessType = BusinessType.UPDATE)
    @Anonymous
    @ApiOperation("上传课程资料")
    @PostMapping("/material/upload")
    public R<Map<String, Object>> uploadMaterial(
            @ApiParam("资料文件") @RequestParam("file") MultipartFile file,
            @ApiParam("资料名称") @RequestParam(value = "materialName", required = false) String materialName) {
        Map<String, Object> materialInfo = courseManageService.uploadMaterial(file, materialName);
        return R.ok(materialInfo);
    }

    /**
     * 上传课程封面：返回封面信息
     */
    @Log(title = "课程封面", businessType = BusinessType.UPDATE)
    @Anonymous
    @ApiOperation("上传课程封面")
    @PostMapping("/cover/upload")
    public R<Map<String, Object>> uploadCourseCover(
            @ApiParam("封面文件") @RequestParam("file") MultipartFile file,
            @ApiParam("封面名称") @RequestParam(value = "coverName", required = false) String coverName) {
        Map<String, Object> coverInfo = courseManageService.uploadCourseCover(file, coverName);
        return R.ok(coverInfo);
    }

    /**
     * 批量获取课程内容图片临时URL
     * 用于富文本 textContent 中图片的临时 URL 刷新，每次加载页面时调用
     * 接收相对路径列表，返回 相对路径 -> 临时URL 的映射
     */
    @Anonymous
    @ApiOperation("批量获取课程内容图片临时URL")
    @PostMapping("/content/image-urls")
    public R<Map<String, String>> batchGetContentImageUrls(
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> paths = (List<String>) body.get("paths");
        Integer minute = body.get("minute") instanceof Number ? ((Number) body.get("minute")).intValue() : 1440;
        if (paths == null || paths.isEmpty()) {
            return R.ok(new HashMap<>());
        }
        // 限制最多50个，有效期最长1440分钟（24小时）
        if (paths.size() > 50) {
            paths = paths.subList(0, 50);
        }
        int validMinute = Math.min(minute, 1440);
        return R.ok(courseManageService.batchGetContentImageUrls(paths, validMinute));
    }

    /**
     * 获取课程资料列表
     */
    @Anonymous
    @ApiOperation("获取课程资料列表")
    @GetMapping("/{courseId}/materials")
    public R<List<CourseMaterialVO>> getCourseMaterials(
            @ApiParam("课程 ID") @PathVariable Long courseId) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();
        return R.ok(courseManageService.getCourseMaterials(courseId, userId));
    }

    /**
     * 删除文件
     * TODO S3 删除文件接口 未开发
     */
    @Log(title = "课程资料", businessType = BusinessType.DELETE)
    //@PreAuthorize("@ss.hasPermi('system:course:material:delete')")
    @ApiOperation("删除课程资料")
    @DeleteMapping("/material/{materialId}")
    @Anonymous

    public R<Void> deleteMaterial(
            @ApiParam("资料 ID") @PathVariable Long materialId) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();
        courseManageService.deleteMaterial(materialId, userId);
        return R.ok();
    }



    /**
     * 编辑：添加章节/课时
     * 为已存在的课程追加新的章节或课时内容
     */
    @Log(title = "课程章节", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('system:course:section:add')")
    @ApiOperation("添加章节/课时")
    @PostMapping("/{courseId}/section")
    @Anonymous
    public R<Long> addSection(
            @ApiParam("课程ID") @PathVariable Long courseId,
            @ApiParam("章节DTO") @Valid @RequestBody SectionCreateDTO sectionCreateDTO) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();
        Long sectionId = courseManageService.addSection(courseId, sectionCreateDTO, userId);
        return R.ok(sectionId);
    }
    
    /**
     * 修改课程
     */
    @Log(title = "课程管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('system:course:edit')")
    @ApiOperation("修改课程")
    @PutMapping
    @Anonymous
    public R<Void> edit(@RequestBody OshCourse course) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();
        courseManageService.updateCourse(course, userId);
        return R.ok();
    }
    
    /**
     * 删除课程
     */
    @Log(title = "课程管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('system:course:remove')")
    @ApiOperation("删除课程")
    @DeleteMapping("/{courseId}")
    @Anonymous
    public R<Void> remove(@ApiParam("课程 ID") @PathVariable Long courseId) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();
        courseManageService.deleteCourse(courseId, userId);
        return R.ok();
    }

    /**
     * 检查购买权限
     * 检查用户是否有权限观看此章节
     */
    @Anonymous
    @ApiOperation("检查购买权限")
    @GetMapping("/section/{sectionId}/access")
    public R<SectionAccessVO> checkSectionAccess(
            @ApiParam("章节ID") @PathVariable Long sectionId) {
        // 匿名接口：安全获取用户ID（未登录时为null）
        Long userId = null;
        try {
            userId = getLoginUser().getUserId();
        } catch (Exception e) {
            // 未登录用户，userId保持为null
        }
        return R.ok(courseManageService.checkSectionAccess(sectionId, userId));
    }


    /**
     * 获取课程大纲
     */
    @Anonymous
    @ApiOperation("获取课程大纲")
    @GetMapping("/{courseId}/sections")
    public R<List<CourseSectionVO>> getCourseSections(
            @ApiParam("课程 ID") @PathVariable Long courseId) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();
        return R.ok(courseManageService.getCourseSections(courseId, userId));
    }
    
    /**
     * 立即学习（试看免费章节）
     */
    @Anonymous
    @ApiOperation("立即学习")
    @PostMapping("/learn")
    public R<SectionDTO> learnSection(@RequestBody Map<String, Long> params) {
        Long courseId = params.get("courseId");
        Long sectionId = params.get("sectionId");
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();
        return R.ok(courseManageService.learnSection(courseId, sectionId, userId));
    }
    
    /**
     * 更新学习进度
     */
    @Log(title = "课程学习", businessType = BusinessType.UPDATE)
    @ApiOperation("更新学习进度")
    @PostMapping("/progress/update")
    public R<Void> updateProgress(@RequestBody Map<String, Object> params) {
        Long courseId = (Long) params.get("courseId");
        Long sectionId = (Long) params.get("sectionId");
        Double progress = (Double) params.get("progress");
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();
        courseManageService.updateProgress(courseId, sectionId, userId, progress);
        return R.ok();
    }
    
    
    // ==================== 视频播放接口 ====================
    
    /**
     * 获取章节视频信息
     * 返回视频URL、时长、封面、分辨率等信息
     */
    @Anonymous
    @ApiOperation("获取章节视频信息")
    @GetMapping("/section/{sectionId}/video")
    public R<SectionVideoVO> getSectionVideo(
            @ApiParam("章节ID") @PathVariable Long sectionId) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();
        return R.ok(courseManageService.getSectionVideo(sectionId, userId));
    }
    
    /**
     * 更新播放进度
     * 提交当前播放位置、进度百分比
     */
    @Log(title = "视频播放", businessType = BusinessType.UPDATE)
    @ApiOperation("更新播放进度")
    @PutMapping("/section/{sectionId}/progress")
    public R<Void> updatePlayProgress(
            @ApiParam("章节ID") @PathVariable Long sectionId,
            @RequestBody ProgressDTO progressDTO) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();
        courseManageService.updatePlayProgress(sectionId, progressDTO, userId);
        return R.ok();
    }
    
    /**
     * 获取播放历史
     * 获取用户上次播放位置
     */
    @ApiOperation("获取播放历史")
    @GetMapping("/section/{sectionId}/progress")
    public R<SectionProgressVO> getPlayProgress(
            @ApiParam("章节ID") @PathVariable Long sectionId) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();
        return R.ok(courseManageService.getPlayProgress(sectionId, userId));
    }
    
    /**
     * 记录观看完成
     * 标记章节学习完成
     */
    @Log(title = "视频播放", businessType = BusinessType.UPDATE)
    @ApiOperation("记录观看完成")
    @PostMapping("/section/{sectionId}/complete")
    public R<Long> markSectionComplete(
            @ApiParam("章节ID") @PathVariable Long sectionId) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();
        Long examId = courseManageService.markSectionComplete(sectionId, userId);
        return R.ok(examId, examId != null ? "章节已完成，请参加考试" : "章节已完成");
    }

    

    
    // ==================== 课程问答接口 ====================
    
    /**
     * 提问（课程小节内）
     */
    @Log(title = "课程问答", businessType = BusinessType.INSERT)
    @ApiOperation("提问")
    @PostMapping("/question")
    public R<Long> askQuestion(@RequestBody QuestionDTO questionDTO) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();
        Long questionId = courseManageService.askQuestion(questionDTO, userId);
        return R.ok(questionId);
    }
    
    /**
     * 回答问题（仅课程服务人员）
     */
    @Log(title = "课程问答", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('system:course:question:answer')")
    @ApiOperation("回答问题")
    @PostMapping("/question/{questionId}/answer")
    public R<Void> answerQuestion(
            @ApiParam("问题 ID") @PathVariable Long questionId,
            @RequestBody Map<String, String> params) {
        String answerContent = params.get("answerContent");
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();

        courseManageService.answerQuestion(questionId, answerContent, userId);
        return R.ok();
    }
    
    /**
     * 跳转到问答板块问题详情
     */
    @Anonymous
    @ApiOperation("获取问题详情")
    @GetMapping("/question/{questionId}")
    public R<QuestionDTO> getQuestionDetail(
            @ApiParam("问题 ID") @PathVariable Long questionId) {
        return R.ok(courseManageService.getQuestionDetail(questionId));
    }
    
    
    // ==================== 课程评价接口 ====================
    
    /**
     * 提交课程评价
     */
    @Log(title = "课程评价", businessType = BusinessType.INSERT)
    @ApiOperation("提交课程评价")
    @PostMapping("/review")
    public R<Void> submitReview(@RequestBody ReviewDTO reviewDTO) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();
        courseManageService.submitReview(reviewDTO, userId);
        return R.ok();
    }
    
    /**
     * 获取课程评价统计
     */
    @Anonymous
    @ApiOperation("获取课程评价统计")
    @GetMapping("/{courseId}/reviews/statistics")
    public R<Map<String, Object>> getReviewStatistics(
            @ApiParam("课程 ID") @PathVariable Long courseId) {
        return R.ok(courseManageService.getReviewStatistics(courseId));
    }
    
    
    // ==================== 课程服务人员接口 ====================
    
    /**
     * 申请成为课程服务人员
     */
    @Log(title = "课程服务人员", businessType = BusinessType.INSERT)
    @ApiOperation("申请成为课程服务人员")
    @PostMapping("/staff/apply")
    public R<Long> applyStaff(
            @RequestBody Map<String, Object> params) {
        Long courseId = (Long) params.get("courseId");
        String staffType = (String) params.get("staffType");
        Integer examScore = (Integer) params.get("examScore");
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();

        Long applyId = courseManageService.applyStaff(courseId, staffType, examScore, userId);
        return R.ok(applyId);
    }
    
    /**
     * 审核服务人员申请（管理员）
     */
    @Log(title = "课程服务人员", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('system:course:staff:audit')")
    @ApiOperation("审核服务人员申请")
    @PostMapping("/staff/{applyId}/audit")
    public R<Void> auditStaff(
            @ApiParam("申请 ID") @PathVariable Long applyId,
            @RequestBody Map<String, String> params) {
        String auditStatus = params.get("auditStatus");
        String auditRemark = params.get("auditRemark");
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();
        courseManageService.auditStaff(applyId, auditStatus, auditRemark, userId);
        return R.ok();
    }
    
    /**
     * 获取课程服务人员列表
     */
    @Anonymous
    @ApiOperation("获取课程服务人员列表")
    @GetMapping("/{courseId}/staffs")
    public R<List<Map<String, Object>>> getCourseStaffs(
            @ApiParam("课程 ID") @PathVariable Long courseId) {
        return R.ok(courseManageService.getCourseStaffs(courseId));
    }
    
    // ==================== 课程收藏接口 ====================
    
    /**
     * 添加课程收藏
     */
    @ApiOperation("添加课程收藏")
    @PostMapping("/{courseId}/favorite")
    public R<Void> addFavorite(
            @ApiParam("课程 ID") @PathVariable Long courseId) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();
        courseManageService.addFavorite(courseId, userId);
        return R.ok();
    }
    
    /**
     * 取消课程收藏
     */
    @ApiOperation("取消课程收藏")
    @DeleteMapping("/{courseId}/favorite")
    public R<Void> removeFavorite(
            @ApiParam("课程 ID") @PathVariable Long courseId) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();
        courseManageService.removeFavorite(courseId, userId);
        return R.ok();
    }
    
    /**
     * 检查是否已收藏课程
     */
    @ApiOperation("检查是否已收藏课程")
    @GetMapping("/{courseId}/favorite")
    public R<Boolean> checkFavorited(
            @ApiParam("课程 ID") @PathVariable Long courseId) {
        OshUser currentOshUser = UserContextUtil.getCurrentUser();

        Long userId = currentOshUser.getId();
        return R.ok(courseManageService.checkFavorited(courseId, userId));
    }
}

package com.backstage.system.service.impl.course;

import com.backstage.common.core.page.TableDataInfo;
import com.backstage.common.enums.UploadPathEnum;
import com.backstage.common.exception.ServiceException;
import com.backstage.common.utils.DateUtils;
import com.backstage.common.utils.StringUtils;
import com.backstage.common.utils.PageUtils;
import com.backstage.common.utils.bean.BeanUtils;
import com.backstage.system.constants.CourseUploadConstants;
import com.backstage.system.constants.CourseLearningConstants;
import com.backstage.system.constants.CourseQuestionConstants;
import com.backstage.system.constants.CourseReviewConstants;
import com.backstage.system.constants.CourseTagConstants;
import com.backstage.system.domain.course.*;
import com.backstage.system.domain.dto.*;
import com.backstage.system.domain.vo.*;
import com.backstage.system.enums.CourseResourceEnum;
import com.backstage.system.mapper.course.OshCourseMapper;
import com.backstage.system.mapper.course.OshCourseSectionMapper;
import com.backstage.system.mapper.course.OshCourseMaterialMapper;
import com.backstage.system.mapper.course.OshCourseQuestionMapper;
import com.backstage.system.mapper.course.OshCourseTagMapper;
import com.backstage.system.mapper.course.OshUserCourseProgressMapper;
import com.backstage.system.mapper.course.OshCourseStaffMapper;
import com.backstage.system.mapper.course.OshCourseReviewMapper;
import com.backstage.system.domain.fava.OshFava;
import com.backstage.system.mapper.fava.OshFavaMapper;
import com.backstage.system.mapper.course.OshCourseOrderMapper;
import com.backstage.system.service.course.ICourseManageService;
import com.backstage.system.service.common.OssService;
import com.backstage.system.utils.OssUtil;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 课程管理 Service 业务层处理
 * 
 * @author ruoyi
 * @date 2026-03-24
 */
@Service
public class CourseManageServiceImpl implements ICourseManageService {
    
    private static final Logger log = LoggerFactory.getLogger(CourseManageServiceImpl.class);
    
    @Autowired
    private OshCourseMapper courseMapper;
    
    @Autowired
    private OshCourseSectionMapper sectionMapper;
    
    @Autowired
    private OshCourseMaterialMapper materialMapper;
    
    @Autowired
    private OshCourseQuestionMapper questionMapper;
    
    @Autowired
    private OshCourseTagMapper tagMapper;
    
    @Autowired
    private OshUserCourseProgressMapper progressMapper;
    
    @Autowired
    private OshCourseStaffMapper staffMapper;
    
    @Autowired
    private OssService ossService;
    
    @Autowired
    private OssUtil ossUtil;

    
    // ==================== 课程查询接口实现 ====================
    private OshCourseReviewMapper reviewMapper;
    
    @Autowired
    private OshFavaMapper favaMapper;
    
    @Autowired
    private OshCourseOrderMapper orderMapper;


    
    // ==================== 课程查询接口实现 ====================
    
    /**
     * 查询课程列表：支持多标签筛选、支持关键字模糊搜索、按标签使用量降序排列，热门课程优先展示
     * @param courseDTO 查询条件 DTO
     * @return 分页课程列表
     */
    @Override
    public TableDataInfo selectCourseList(CourseQueryDTO courseDTO) {
   
        Map<String, Object> params = new HashMap<>();
        if (courseDTO.getTagIds() != null && !courseDTO.getTagIds().isEmpty()) {
            params.put("tagIds", courseDTO.getTagIds());
        }
        
        // TODO ES 实现关键字搜索
        if (StringUtils.isNotEmpty(courseDTO.getKeyword())) {
            params.put("keyword", "%" + courseDTO.getKeyword() + "%");
        }
        
        // 4. 设置排序字段和方式
        params.put("sortBy", courseDTO.getSortBy());
        params.put("sortOrder", courseDTO.getSortOrder());
        

        PageUtils.startPage();
        List<OshCourse> list = courseMapper.selectCourseListByCondition(params);
        return new TableDataInfo(list, new com.github.pagehelper.PageInfo<OshCourse>(list).getTotal());
    }
    
    /**
     * 获取课程标签列表（供前端多选下拉框使用）
     * 
     * @return 标签列表（包含 id、name、useCount）
     */
    @Override
    public List<Map<String, Object>> selectTagList() {
        return tagMapper.selectAllTags();
    }
    
    /**
     * 获取课程详情
     * 语法逻辑：
     * 1. 查询课程基本信息
     * 2. 查询章节列表
     * 3. 查询评价统计
     * 4. 判断用户是否已购买
     * 
     * 实现效果：
     * - 返回课程完整信息，包括服务周期、服务内容
     * - 显示前几节免费试听章节
     * - 展示好评、中评、差评数量
     * - 标记用户是否已购买
     * 
     * @param courseId 课程 ID
     * @param userId 用户 ID（用于判断是否购买）
     * @return 课程详情 VO
     */
    @Override
    public CourseDetailVO getCourseDetail(Long courseId, Long userId) {
        // 1. 查询课程基本信息
        OshCourse course = courseMapper.selectCourseById(courseId);
        if (course == null) {
            throw new ServiceException("课程不存在");
        }
        
        // 2. 构建课程详情 VO
        CourseDetailVO vo = new CourseDetailVO();
        vo.setId(course.getId());
        vo.setTitle(course.getTitle());
        vo.setCover(course.getCover());
        vo.setTryContent(course.getTryContent());
        vo.setPrice(course.getPrice() != null ? course.getPrice().toString() : "0.00");
        vo.setTPrice(course.getTPrice() != null ? course.getTPrice().toString() : "0.00");
        vo.setType(course.getType());
        CourseResourceEnum resourceEnum = CourseResourceEnum.fromCode(course.getResourceType());
        vo.setResourceType(resourceEnum == null ? course.getResourceType() : resourceEnum.getDesc());

        // 3. 设置服务周期和服务内容（从扩展字段或配置表获取）
        vo.setServiceCycle("永久有效");
        vo.setServiceContent("包含答疑、资料下载等");
        
        // 4. 查询评价统计
        Map<String, Object> reviewStats = getReviewStatistics(courseId);
        vo.setGoodCount((Integer) reviewStats.getOrDefault("goodCount", 0));
        vo.setMediumCount((Integer) reviewStats.getOrDefault("mediumCount", 0));
        vo.setBadCount((Integer) reviewStats.getOrDefault("badCount", 0));
        
        // 5. 判断用户是否已购买
        boolean isPurchased = checkUserPurchased(courseId, userId);
        vo.setIsBuy(isPurchased);
        
        // 6. 判断用户是否已收藏（从其他地方获取收藏状态）
        // 如果需要获取收藏状态，可从收藏表中查询
        Boolean isFavorited = false;
        vo.setIsfava(isFavorited);
        
        // 7. 查询章节列表（仅显示前几节免费章节用于试看）
        List<CourseSectionVO> sections = getCourseSections(courseId, userId);
        vo.setSections(sections);
        
        return vo;
    }


    // ==================== 封面、视频、资料上传接口实现 ====================

    /**
     * 上传课程封面图片
     * - 支持常见图片格式（bmp/gif/jpg/jpeg/png）
     * - 返回封面信息（名称、URL、大小、类型）
     *
     * @param file 封面文件
     * @param coverName 封面名称
     * @return 封面信息（名称、URL、大小、类型）
     */
    @Override
    public Map<String, Object> uploadCourseCover(MultipartFile file, String coverName) {
        // 校验文件类型（仅允许图片格式）
        String fileName = file.getOriginalFilename();
        String extension = "";
        if (fileName != null && fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }

        if (!CourseUploadConstants.ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new ServiceException(CourseUploadConstants.IMAGE_FORMAT_ERROR);
        }

        // 校验文件大小
        if (file.getSize() > CourseUploadConstants.MAX_IMAGE_SIZE) {
            throw new ServiceException(CourseUploadConstants.IMAGE_SIZE_ERROR);
        }

        // 调用 OSS 服务上传文件
        String coverUrl;
        String relativePath;
        try {
            // 获取相对路径
            relativePath = ossService.upload(file, UploadPathEnum.COURSE_COVER, "covers");

            // 检查上传结果是否包含错误信息
            if (relativePath == null || CourseUploadConstants.isUploadError(relativePath)) {
                throw new ServiceException(relativePath);
            }
            
            // 生成临时访问URL（有效期24小时 = 1440分钟）
            coverUrl = ossService.getLimitedUrl(relativePath, 1440);

            log.info("课程封面上传 - 相对路径: {}, 临时访问URL: {}", relativePath, coverUrl);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("上传封面失败：" + e.getMessage());
        }

        // 构建返回信息
        Map<String, Object> coverInfo = new HashMap<>();
        coverInfo.put("coverName", StringUtils.defaultIfEmpty(coverName, fileName));
        coverInfo.put("url", coverUrl);              // 临时访问URL（前端显示用）
        coverInfo.put("relativePath", relativePath); // 相对路径（数据库存储用）
        coverInfo.put("size", file.getSize());
        coverInfo.put("type", extension);

        log.info("课程封面上传成功：name={}, 临时URL={}, 相对路径={}, size={}, type={}",
                coverInfo.get("coverName"), coverUrl, relativePath, file.getSize(), extension);
        return coverInfo;
    }

    /**
     * 上传课时视频（指定章节 ID）
     * 实现效果:
     * - 支持 mp4、avi、mov、mkv 等常见视频格式
     * - 返回视频信息（名称、URL、大小、类型）
     *
     * @param file 视频文件
     * @param videoName 视频名称
     * @return 视频信息（名称、URL、大小、类型）
     */
    @Override
    public Map<String, Object> uploadVideo(MultipartFile file, String videoName, Long sectionId) {
        // 1. 校验文件类型（仅允许视频格式）
        String fileName = file.getOriginalFilename();
        String extension = "";
        if (fileName != null && fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }

        if (!CourseUploadConstants.ALLOWED_VIDEO_EXTENSIONS.contains(extension)) {
            throw new ServiceException(CourseUploadConstants.VIDEO_FORMAT_ERROR);
        }

        // 2. 校验文件大小
        if (file.getSize() > CourseUploadConstants.MAX_VIDEO_SIZE) {
            throw new ServiceException(CourseUploadConstants.VIDEO_SIZE_ERROR);
        }

        // 3. 重新上传：先删 OSS 旧视频，避免堆积无用文件
        if (sectionId != null) {
            deleteSectionVideoFromOss(sectionId);
        }

        // 4. 直传 OSS（与资料上传一致，跳过 osh_oss_operation_log 写库，减少上传耗时）
        String videoUrl;
        String relativePath;
        try {
            relativePath = uploadCourseVideoToOss(file);
            videoUrl = ossService.getLimitedUrl(relativePath, 360);
            log.info("课时视频上传 - 相对路径: {}, 临时访问URL: {}", relativePath, videoUrl);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("上传视频失败：" + e.getMessage());
        }

        // 5. 构建返回信息
        Map<String, Object> videoInfo = new HashMap<>();
        videoInfo.put("videoName", StringUtils.defaultIfEmpty(videoName, fileName));
        videoInfo.put("url", videoUrl);
        videoInfo.put("relativePath", relativePath);
        videoInfo.put("size", file.getSize());
        videoInfo.put("type", extension);

        log.info("视频上传成功：name={}, sectionId={}, 相对路径={}, size={}",
                videoInfo.get("videoName"), sectionId, relativePath, file.getSize());
        return videoInfo;
    }



    /**
     * 上传课程资料
     * 校验文件类型（仅允许压缩包 zip/rar/tar/gz）
     * 存储到指定目录
     * 返回资料信息（名称、URL、大小、类型）
     *
     * @param file 资料文件
     * @param materialName 资料名称
     * @return 资料信息（名称、URL、大小、类型）
     */
    @Override
    public Map<String, Object> uploadMaterial(MultipartFile file, String materialName) {
        // 校验文件类型
        String fileName = file.getOriginalFilename();
        String extension = "";
        if (fileName != null && fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }

        if (!CourseUploadConstants.ALLOWED_ARCHIVE_EXTENSIONS.contains(extension)) {
            throw new ServiceException(CourseUploadConstants.ARCHIVE_FORMAT_ERROR);
        }

        if (file.getSize() > 1024L * 1024 * 100) {
            throw new ServiceException("资料大小不能超过100MB");
        }

        try {
            // 课程资料直传 OSS，不走 OssService.upload（避免 osh_oss_operation_log 写库失败拖垮上传）
            String relativePath = uploadCourseMaterialToOss(file);
            
            // 生成临时访问URL（有效期12小时 = 720分钟）
            String materialUrl = ossService.getLimitedUrl(relativePath, 720);

            log.info("课程资料上传 - 相对路径: {}, 临时访问URL: {}", relativePath, materialUrl);

            // 构建返回信息
            Map<String, Object> materialInfo = new HashMap<>();
            materialInfo.put("materialName", StringUtils.defaultIfEmpty(materialName, fileName));
            materialInfo.put("url", materialUrl);              // 临时访问URL（前端显示用）
            materialInfo.put("relativePath", relativePath);    // 相对路径（数据库存储用）
            materialInfo.put("size", file.getSize());
            materialInfo.put("type", extension);

            log.info("课程资料上传成功：name={}, 临时URL={}, 相对路径={}, size={}, type={}",
                    materialInfo.get("materialName"), materialUrl, relativePath, file.getSize(), extension);

            return materialInfo;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("上传资料失败：" + e.getMessage());
        }
    }

    /**
     * 课程视频直传 OSS（路径规则与 OssService 保持一致，但不写 osh_oss_operation_log）。
     */
    private String uploadCourseVideoToOss(MultipartFile file) throws Exception {
        String ym = DateUtils.getDate().substring(0, 7).replace("-", "");
        String customPath = UploadPathEnum.COURSE_VIDEO.getPath() + "videos/" + ym + "/";
        String objectKey = customPath + UUID.randomUUID() + "_" + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        AmazonS3Client s3 = ossUtil.createS3Client();
        PutObjectRequest request = new PutObjectRequest(
                ossUtil.getOssProperties().getBucketName(),
                objectKey,
                file.getInputStream(),
                metadata
        );
        s3.putObject(request);
        return objectKey;
    }

    /**
     * 重新上传前删除小节关联的旧视频（仅删 OSS 对象，DB 由 save 接口更新）。
     */
    private void deleteSectionVideoFromOss(Long sectionId) {
        if (sectionId == null) {
            return;
        }
        List<OshCourseSection> sections = sectionMapper.selectSectionsByIds(Collections.singletonList(sectionId));
        if (sections == null || sections.isEmpty()) {
            return;
        }
        String relativePath = sections.get(0).getMediaUrl();
        if (StringUtils.isEmpty(relativePath)
                || relativePath.contains("pending")
                || relativePath.startsWith("http://")
                || relativePath.startsWith("https://")) {
            return;
        }
        String objectKey = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        try {
            boolean deleted = ossUtil.deleteFile(objectKey);
            log.info("重新上传删除旧视频: sectionId={}, key={}, deleted={}", sectionId, objectKey, deleted);
        } catch (Exception e) {
            log.warn("重新上传删除旧视频失败: sectionId={}, key={}, error={}", sectionId, objectKey, e.getMessage());
        }
    }

    /**
     * 课程资料直传 OSS（路径规则与 OssService 保持一致，但不写 osh_oss_operation_log）。
     */
    private String uploadCourseMaterialToOss(MultipartFile file) throws Exception {
        String ym = DateUtils.getDate().substring(0, 7).replace("-", "");
        String customPath = UploadPathEnum.COURSE_MATERIAL.getPath() + "materials/" + ym + "/";
        String objectKey = customPath + UUID.randomUUID() + "_" + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        AmazonS3Client s3 = ossUtil.createS3Client();
        PutObjectRequest request = new PutObjectRequest(
                ossUtil.getOssProperties().getBucketName(),
                objectKey,
                file.getInputStream(),
                metadata
        );
        s3.putObject(request);
        return objectKey;
    }


    /**
     * 新增课程
     * 保存 章节、标签关联关系
     * 
     * @param course 课程信息
     * @param userId 用户 ID
     * @return 课程 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertCourse(OshCourse course, Long userId) {
        // 1. 设置课程默认值
        course.setCreateTime(DateUtils.getNowDate());
        course.setUpdateTime(DateUtils.getNowDate());
        
        // 2. 插入课程信息
        int result = courseMapper.insertCourse(course);
        if (result <= 0) {
            throw new ServiceException("新增课程失败");
        }
        
        // 3. 获取生成的课程 ID
        Long courseId = course.getId();
        
        // 4. 保存标签关联关系
        if (course.getTagIds() != null && course.getTagIds().length > 0) {
            for (Long tagId : course.getTagIds()) {
                Map<String, Object> param = new HashMap<>();
                param.put("courseId", courseId);
                param.put("tagId", tagId);
                tagMapper.insertCourseTagRelation(param);
                
                // 更新标签使用次数
                tagMapper.incrementUsageCount(tagId);
            }
        }
        
        return courseId;
    }
    
    /**
     * 修改课程
     * 1. 更新课程章节信息
     * 2. 更新标签关联关系
     * 
     * 实现效果：
     * - 删除旧标签关联，插入新标签关联
     * - 事务保证数据一致性
     * - 权限控制由 Controller 层@RequiresPermissions 注解处理
     * 
     * @param course 课程信息
     * @param userId 用户 ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCourse(OshCourse course, Long userId) {
        // 1. 更新时间
        course.setUpdateTime(DateUtils.getNowDate());
        
        // 2. 更新课程信息
        int result = courseMapper.updateCourse(course);
        
        // 3. 更新标签关联（先删后增）
        if (course.getTagIds() != null) {
            // 删除旧的标签关联
            tagMapper.deleteCourseTagRelationByCourseId(course.getId());
            
            // 插入新的标签关联
            if (course.getTagIds().length > 0) {
                for (Long tagId : course.getTagIds()) {
                    Map<String, Object> param = new HashMap<>();
                    param.put("courseId", course.getId());
                    param.put("tagId", tagId);
                    tagMapper.insertCourseTagRelation(param);
                    
                    // 更新标签使用次数
                    tagMapper.incrementUsageCount(tagId);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 删除课程
     * 语法逻辑：
     * 1. 级联删除章节、资料、问答等
     * 
     * 实现效果：
     * - 物理删除课程及相关所有数据
     * - 事务保证数据一致性
     * - 权限控制由 Controller 层@RequiresPermissions 注解处理
     * 
     * @param courseId 课程 ID
     * @param userId 用户 ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCourse(Long courseId, Long userId) {
        // 1. 级联删除相关数据
        // 删除章节
        sectionMapper.deleteSectionsByCourseId(courseId);
        
        // 删除资料
        materialMapper.deleteMaterialsByCourseId(courseId);
        
        // 删除问答
        questionMapper.deleteQuestionsByCourseId(courseId);
        
        // 删除标签关联
        tagMapper.deleteCourseTagRelationByCourseId(courseId);
        
        // 删除学习进度
        progressMapper.deleteProgressByCourseId(courseId);
        
        // 删除服务人员记录
        staffMapper.deleteStaffsByCourseId(courseId);
        
        // 删除评价
        reviewMapper.deleteReviewsByCourseId(courseId);
        
        // 2. 删除课程
        return courseMapper.deleteCourseById(courseId);
    }
    
    // ==================== 课程章节接口实现 ====================
    
    /**
     * 获取课程大纲
     * 语法逻辑：
     * 1. 查询所有章节
     * 2. 标记免费/付费状态
     * 3. 计算学习进度
     * 
     * 实现效果：
     * - 显示所有章节，按排序序号排列
     * - 未购买时，付费章节显示锁定状态
     * - 已购买时，显示学习进度（已学/未学）
     * 
     * @param courseId 课程 ID
     * @param userId 用户 ID
     * @return 章节列表 VO
     */
    @Override
    public List<CourseSectionVO> getCourseSections(Long courseId, Long userId) {
        // 1. 查询所有章节（按排序序号）
        List<Map<String, Object>> sections = sectionMapper.selectSectionsByCourseId(courseId);
        
        // 2. 判断用户是否已购买
        boolean isPurchased = checkUserPurchased(courseId, userId);
        
        // 3. 查询用户学习进度
        Map<String, Object> progressMap = new HashMap<>();
        if (isPurchased && userId != null) {
            progressMap = progressMapper.selectProgressByUserIdAndCourseId(userId, courseId);
        }
        
        // 4. 封装返回结果
        List<CourseSectionVO> result = new ArrayList<>();
        for (Map<String, Object> section : sections) {
            CourseSectionVO vo = new CourseSectionVO();
            vo.setId((Long) section.get("id"));
            vo.setTitle((String) section.get("title"));
            vo.setSectionType((String) section.get("type"));
            vo.setDuration((Integer) section.get("duration"));
            vo.setFree((Boolean) section.get("free_flag"));
            
            // 判断是否已学习
            if (progressMap != null && progressMap.containsKey(vo.getId())) {
                vo.setLearned(true);
            } else {
                vo.setLearned(false);
            }
            
            // 查询该章节的问题数量
            int questionCount = questionMapper.countQuestionsBySectionId(vo.getId());
            vo.setHasQuestion(questionCount > 0);
            vo.setQuestionCount(questionCount);
            
            // 未购买时，付费章节显示锁定
            if (!isPurchased && !vo.getFree()) {
                vo.setLocked(true);
            } else {
                vo.setLocked(false);
            }
            
            result.add(vo);
        }
        
        return result;
    }
    
    /**
     * 立即学习（试看免费章节）
     * 语法逻辑：
     * 1. 查询章节信息
     * 2. 判断是否免费
     * 3. 检查是否已购买
     * 
     * 实现效果：
     * - 免费章节直接返回视频 URL 或文本内容
     * - 付费章节提示需要购买
     * 
     * @param courseId 课程 ID
     * @param sectionId 章节 ID
     * @param userId 用户 ID
     * @return 章节内容
     */
    @Override
    public SectionDTO learnSection(Long courseId, Long sectionId, Long userId) {
        // 1. 查询章节信息
        Map<String, Object> section = sectionMapper.selectSectionById(sectionId);
        if (section == null) {
            throw new ServiceException("章节不存在");
        }
        
        // 2. 校验章节是否属于该课程
        if (!courseId.equals(section.get("course_id"))) {
            throw new ServiceException("章节不属于该课程");
        }
        
        // 3. 构建返回结果
        SectionDTO dto = new SectionDTO();
        dto.setId(sectionId);
        
        // 4. 判断是否免费
        boolean isFree = (Boolean) section.get("free_flag");
        dto.setFree(isFree);
        
        // 5. 如果免费，直接返回内容
        if (isFree) {
            dto.setCanLearn(true);
            dto.setNeedPurchase(false);
            
            String sectionType = (String) section.get("type");
            if ("video".equals(sectionType)) {
                dto.setVideoUrl((String) section.get("media_url"));
            } else if ("text".equals(sectionType)) {
                dto.setTextContent((String) section.get("media_url"));
            }
        } else {
            // 6. 付费章节，检查是否已购买
            boolean isPurchased = checkUserPurchased(courseId, userId);
            if (isPurchased) {
                dto.setCanLearn(true);
                dto.setNeedPurchase(false);
                
                String sectionType = (String) section.get("type");
                if ("video".equals(sectionType)) {
                    dto.setVideoUrl((String) section.get("media_url"));
                } else if ("text".equals(sectionType)) {
                    dto.setTextContent((String) section.get("media_url"));
                }
            } else {
                dto.setCanLearn(false);
                dto.setNeedPurchase(true);
            }
        }
        
        return dto;
    }
    
    /**
     * 更新学习进度
     * 语法逻辑：
     * 1. 查询当前进度
     * 2. 更新进度信息
     * 3. 计算完成百分比
     * 
     * 实现效果：
     * - 记录用户学习的每个章节
     * - 自动计算整体学习进度百分比
     * - 学完所有章节后标记为已完成
     * 
     * @param courseId 课程 ID
     * @param sectionId 章节 ID
     * @param userId 用户 ID
     * @param progress 学习进度百分比
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProgress(Long courseId, Long sectionId, Long userId, Double progress) {
        // 1. 查询当前进度记录
        Map<String, Object> currentProgress = progressMapper.selectProgressByUserIdAndCourseId(userId, courseId);
        
        // 2. 计算总章节数
        int totalSections = sectionMapper.countSectionsByCourseId(courseId);
        
        if (currentProgress == null) {
            // 3. 首次学习，创建进度记录
            Map<String, Object> params = new HashMap<>();
            params.put("userId", userId);
            params.put("courseId", courseId);
            params.put("sectionId", sectionId);
            params.put("learnedSectionCount", 1);
            params.put("totalSectionCount", totalSections);
            params.put("progressPercent", progress);
            params.put("lastLearnTime", DateUtils.getNowDate());
            
            // 判断是否完成
            if (progress >= 100.0) {
                params.put("isCompleted", 1);
                params.put("completeTime", DateUtils.getNowDate());
            } else {
                params.put("isCompleted", 0);
            }
            
            progressMapper.insertProgress(params);
        } else {
            // 4. 更新进度记录
            int learnedCount = (Integer) currentProgress.get("learned_section_count");
            
            // 如果该章节未学过，增加已学章节数
            if (!sectionId.equals(currentProgress.get("section_id"))) {
                learnedCount++;
            }
            
            Map<String, Object> params = new HashMap<>();
            params.put("userId", userId);
            params.put("courseId", courseId);
            params.put("sectionId", sectionId);
            params.put("learnedSectionCount", learnedCount);
            params.put("progressPercent", progress);
            params.put("lastLearnTime", DateUtils.getNowDate());
            
            // 判断是否完成
            if (learnedCount >= totalSections) {
                params.put("isCompleted", 1);
                params.put("completeTime", DateUtils.getNowDate());
            } else {
                params.put("isCompleted", 0);
            }
            
            progressMapper.updateProgress(params);
        }
    }
    
    // ==================== 视频播放接口实现 ====================
    
    /**
     * 获取章节视频信息
     * 语法逻辑：
     * 1. 查询章节详情
     * 2. 检查访问权限
     * 3. 查询学习进度
     * 
     * 实现效果：
     * - 返回视频URL、时长、封面、分辨率等信息
     * - 包含当前播放进度
     * - 无权限时不返回视频URL
     * 
     * @param sectionId 章节ID
     * @param userId 用户ID（可为空）
     * @return 章节视频信息 VO
     */
    @Override
    public SectionVideoVO getSectionVideo(Long sectionId, Long userId) {
        // 1. 查询章节视频详情
        Map<String, Object> section = sectionMapper.selectSectionVideoById(sectionId);
        if (section == null) {
            throw new ServiceException("章节不存在");
        }
        
        Long courseId = (Long) section.get("course_id");
        
        // 2. 检查章节状态
        Integer sectionStatus = (Integer) section.get("status");
        if (sectionStatus == null || sectionStatus != 1) {
            throw new ServiceException("该章节已下架");
        }
        
        // 3. 构建返回结果
        SectionVideoVO vo = new SectionVideoVO();
        vo.setSectionId(sectionId);
        vo.setTitle((String) section.get("title"));
        vo.setCourseId(courseId);
        vo.setCourseName((String) section.get("course_name"));
        vo.setDuration((Integer) section.get("duration"));
        vo.setCover((String) section.get("cover"));
        vo.setVideoCodec((String) section.get("video_codec"));
        vo.setVideoResolution((String) section.get("video_resolution"));
        vo.setFileSize((Long) section.get("file_size"));
        vo.setSubtitleUrl((String) section.get("subtitle_url"));
        vo.setType((String) section.get("type"));
        vo.setExamId((Long) section.get("exam_id"));
        
        // 格式化时长文本
        if (vo.getDuration() != null) {
            vo.setDurationText(formatDuration(vo.getDuration()));
        }
        
        // 4. 检查是否免费
        Boolean isFree = (Boolean) section.get("free_flag");
        vo.setIsFree(isFree != null && isFree);
        
        // 5. 检查访问权限
        boolean hasAccess = false;
        if (vo.getIsFree()) {
            hasAccess = true;
        } else if (userId != null) {
            hasAccess = checkUserPurchased(courseId, userId);
        }
        vo.setHasAccess(hasAccess);
        
        // 6. 有权限才返回视频URL
        if (hasAccess) {
            vo.setMediaUrl((String) section.get("media_url"));
        }
        
        // 7. 查询学习进度（仅登录用户）
        if (userId != null) {
            Map<String, Object> progress = progressMapper.selectSectionProgress(userId, sectionId);
            if (progress != null) {
                vo.setCurrentProgress((Integer) progress.get("progress"));
                vo.setLastPosition((Integer) progress.get("last_position"));
                vo.setStatus((Integer) progress.get("status"));
                vo.setIsCompleted((Boolean) progress.get("is_completed"));
            } else {
                vo.setCurrentProgress(0);
                vo.setLastPosition(0);
                vo.setStatus(0);
                vo.setIsCompleted(false);
            }
        }
        
        return vo;
    }
    
    /**
     * 更新播放进度
     * 语法逻辑：
     * 1. 查询当前进度
     * 2. 更新或创建进度记录
     * 3. 累加学习时长
     * 
     * 实现效果：
     * - 记录用户视频播放进度
     * - 支持断点续播
     * 
     * @param sectionId 章节ID
     * @param progressDTO 进度更新请求
     * @param userId 用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlayProgress(Long sectionId, ProgressDTO progressDTO, Long userId) {
        // 1. 查询章节信息
        Map<String, Object> section = sectionMapper.selectSectionById(sectionId);
        if (section == null) {
            throw new ServiceException("章节不存在");
        }
        
        Long courseId = (Long) section.get("course_id");
        
        // 2. 查询当前进度
        Map<String, Object> currentProgress = progressMapper.selectSectionProgress(userId, sectionId);
        
        // 3. 判断是否完成
        boolean isCompleted = progressDTO.getProgress() != null && progressDTO.getProgress() >= CourseLearningConstants.PROGRESS_COMPLETED_THRESHOLD;
        Integer status = progressDTO.getStatus();
        if (status == null) {
            status = isCompleted ? CourseLearningConstants.STATUS_COMPLETED : CourseLearningConstants.STATUS_LEARNING;
        }
        
        if (currentProgress == null) {
            // 4. 首次学习，创建进度记录
            Map<String, Object> params = new HashMap<>();
            params.put("userId", userId);
            params.put("courseId", courseId);
            params.put("sectionId", sectionId);
            params.put("status", status);
            params.put("progress", progressDTO.getProgress());
            params.put("lastPosition", progressDTO.getLastPosition());
            params.put("learnTime", progressDTO.getLearnTime());
            params.put(CourseLearningConstants.FIELD_IS_COMPLETED, isCompleted ? CourseLearningConstants.COMPLETED_FLAG_YES : CourseLearningConstants.COMPLETED_FLAG_NO);
            
            progressMapper.insertSectionProgress(params);
        } else {
            // 5. 更新进度记录
            Map<String, Object> params = new HashMap<>();
            params.put("userId", userId);
            params.put("sectionId", sectionId);
            params.put("status", status);
            params.put("progress", progressDTO.getProgress());
            params.put("lastPosition", progressDTO.getLastPosition());
            params.put("learnTime", progressDTO.getLearnTime());
            params.put(CourseLearningConstants.FIELD_IS_COMPLETED, isCompleted ? CourseLearningConstants.COMPLETED_FLAG_YES : CourseLearningConstants.COMPLETED_FLAG_NO);
            
            progressMapper.updateSectionProgress(params);
        }
        
        // 6. 同步更新课程整体进度
        updateCourseOverallProgress(courseId, userId);
    }
    
    /**
     * 获取播放历史
     * 语法逻辑：
     * 1. 查询用户章节学习进度记录
     * 
     * 实现效果：
     * - 返回上次播放位置、进度百分比、学习状态等
     * 
     * @param sectionId 章节ID
     * @param userId 用户ID
     * @return 章节学习进度 VO
     */
    @Override
    public SectionProgressVO getPlayProgress(Long sectionId, Long userId) {
        // 1. 查询进度记录
        Map<String, Object> progress = progressMapper.selectSectionProgress(userId, sectionId);
        
        // 2. 构建返回结果
        SectionProgressVO vo = new SectionProgressVO();
        vo.setSectionId(sectionId);
        
        if (progress != null) {
            vo.setCourseId((Long) progress.get("course_id"));
            vo.setStatus((Integer) progress.get("status"));
            vo.setProgress((Integer) progress.get("progress"));
            vo.setLastPosition((Integer) progress.get("last_position"));
            vo.setLearnTime((Integer) progress.get("learn_time"));
            vo.setWatchCount((Integer) progress.get("watch_count"));
            vo.setIsCompleted((Boolean) progress.get("is_completed"));
            vo.setCompleteTime((Date) progress.get("complete_time"));
            vo.setCreateTime((Date) progress.get("create_time"));
            vo.setUpdateTime((Date) progress.get("update_time"));
        } else {
            // 无记录则返回默认值
            vo.setStatus(0);
            vo.setProgress(0);
            vo.setLastPosition(0);
            vo.setLearnTime(0);
            vo.setWatchCount(0);
            vo.setIsCompleted(false);
        }
        
        return vo;
    }
    
    /**
     * 记录观看完成
     * 语法逻辑：
     * 1. 更新章节学习状态为已完成
     * 2. 更新课程整体进度
     * 3. 检查是否有关联考试
     * 
     * 实现效果：
     * - 标记章节已学完
     * - 更新完成时间
     * - 返回考试ID（如有）
     * 
     * @param sectionId 章节ID
     * @param userId 用户ID
     * @return 考试ID（如有）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long markSectionComplete(Long sectionId, Long userId) {
        // 1. 查询章节信息
        Map<String, Object> section = sectionMapper.selectSectionById(sectionId);
        if (section == null) {
            throw new ServiceException("章节不存在");
        }
        
        Long courseId = (Long) section.get("course_id");
        
        // 2. 更新章节学习状态
        Map<String, Object> params = new HashMap<>();
        params.put(CourseLearningConstants.FIELD_USER_ID, userId);
        params.put(CourseLearningConstants.FIELD_SECTION_ID, sectionId);
        params.put(CourseLearningConstants.FIELD_STATUS, CourseLearningConstants.STATUS_COMPLETED);
        params.put(CourseLearningConstants.FIELD_IS_COMPLETED, CourseLearningConstants.COMPLETED_FLAG_YES);
        
        // 检查是否已有进度记录
        Map<String, Object> progress = progressMapper.selectSectionProgress(userId, sectionId);
        if (progress == null) {
            // 创建进度记录
            Map<String, Object> newParams = new HashMap<>();
            newParams.put(CourseLearningConstants.FIELD_USER_ID, userId);
            newParams.put(CourseLearningConstants.FIELD_COURSE_ID, courseId);
            newParams.put(CourseLearningConstants.FIELD_SECTION_ID, sectionId);
            newParams.put(CourseLearningConstants.FIELD_STATUS, CourseLearningConstants.STATUS_COMPLETED);
            newParams.put(CourseLearningConstants.FIELD_PROGRESS, CourseLearningConstants.PROGRESS_COMPLETED_THRESHOLD);
            newParams.put(CourseLearningConstants.FIELD_LAST_POSITION, CourseLearningConstants.INITIAL_LAST_POSITION);
            newParams.put(CourseLearningConstants.FIELD_LEARN_TIME, CourseLearningConstants.INITIAL_LEARN_TIME);
            newParams.put(CourseLearningConstants.FIELD_IS_COMPLETED, CourseLearningConstants.COMPLETED_FLAG_YES);
            
            progressMapper.insertSectionProgress(newParams);
        } else {
            progressMapper.updateSectionStatus(params);
        }
        
        // 3. 同步更新课程整体进度
        updateCourseOverallProgress(courseId, userId);
        
        // 4. 返回关联考试ID（如有）
        return (Long) section.get("exam_id");
    }
    
    /**
     * 检查购买权限
     * 语法逻辑：
     * 1. 查询章节信息
     * 2. 检查章节是否免费
     * 3. 检查用户是否已购买课程
     * 
     * 实现效果：
     * - 返回用户是否有权限观看此章节
     * 
     * @param sectionId 章节ID
     * @param userId 用户ID（可为空）
     * @return 章节访问权限 VO
     */
    @Override
    public SectionAccessVO checkSectionAccess(Long sectionId, Long userId) {
        // 1. 查询章节信息
        Map<String, Object> section = sectionMapper.selectSectionVideoById(sectionId);
        if (section == null) {
            throw new ServiceException("章节不存在");
        }
        
        Long courseId = (Long) section.get("course_id");
        
        // 2. 构建返回结果
        SectionAccessVO vo = new SectionAccessVO();
        vo.setSectionId(sectionId);
        
        // 3. 检查是否免费
        Boolean isFree = (Boolean) section.get("free_flag");
        vo.setIsFree(isFree != null && isFree);
        
        // 4. 免费章节直接有权限
        if (vo.getIsFree()) {
            vo.setHasAccess(true);
            vo.setNeedPurchase(false);
            vo.setIsPurchased(false);
            return vo;
        }
        
        // 5. 付费章节检查购买状态
        vo.setNeedPurchase(true);
        
        if (userId == null) {
            vo.setHasAccess(false);
            vo.setIsPurchased(false);
            vo.setReason("请先登录");
            return vo;
        }
        
        boolean isPurchased = checkUserPurchased(courseId, userId);
        vo.setIsPurchased(isPurchased);
        vo.setHasAccess(isPurchased);
        
        if (!isPurchased) {
            vo.setReason("请先购买课程");
            // 返回课程价格
            Object price = section.get("price");
            vo.setPrice(price != null ? price.toString() : "0.00");
        }
        
        return vo;
    }
    
    /**
     * 格式化时长
     * 
     * @param seconds 秒数
     * @return 格式化后的时长文本（如：30:00）
     */
    private String formatDuration(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            return "00:00";
        }
        
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }
    }
    
    /**
     * 更新课程整体进度
     * 
     * @param courseId 课程ID
     * @param userId 用户ID
     */
    private void updateCourseOverallProgress(Long courseId, Long userId) {
        // 1. 统计课程总章节数
        int totalSections = sectionMapper.countSectionsByCourseId(courseId);
        
        // 2. 查询已完成的章节数（这里简化处理，实际需要统计）
        Map<String, Object> courseProgress = progressMapper.selectProgressByUserIdAndCourseId(userId, courseId);
        
        // 3. 更新课程进度
        if (courseProgress != null) {
            int learnedCount = (Integer) courseProgress.getOrDefault("learned_section_count", 0);
            Double progressPercent = (totalSections > 0) ? (learnedCount * 100.0 / totalSections) : 0.0;
            
            Map<String, Object> params = new HashMap<>();
            params.put("userId", userId);
            params.put("courseId", courseId);
            params.put("progressPercent", progressPercent);
            params.put("lastLearnTime", DateUtils.getNowDate());
            params.put("isCompleted", learnedCount >= totalSections ? 1 : 0);
            
            progressMapper.updateProgress(params);
        }
    }
    
    // ==================== 课程资料接口实现 ====================
    
    /**
     * 获取课程资料列表
     * 语法逻辑：
     * 1. 检查用户是否已购买
     * 2. 查询资料列表
     * 
     * 实现效果：
     * - 未购买用户不可见资料
     * - 已购买用户可以下载资料
     * 
     * @param courseId 课程 ID
     * @param userId 用户 ID
     * @return 资料列表 VO
     */
    @Override
    public List<CourseMaterialVO> getCourseMaterials(Long courseId, Long userId) {
        // 检查是否已购买
        boolean isPurchased = checkUserPurchased(courseId, userId);
        if (!isPurchased) {
            throw new ServiceException("请先购买课程");
        }
        
        // 查询课程资料列表
        List<Map<String, Object>> materials = materialMapper.selectMaterialsByCourseId(courseId);
        
        // 封装课程资料列表
        List<CourseMaterialVO> result = new ArrayList<>();
        for (Map<String, Object> material : materials) {
            CourseMaterialVO vo = new CourseMaterialVO();
            vo.setId((Long) material.get("id"));
            vo.setMaterialName((String) material.get("material_name"));
            vo.setFileUrl((String) material.get("file_url"));
            vo.setFileSize((Long) material.get("file_size"));
            vo.setDownloadCount((Integer) material.get("download_count"));
            vo.setIsDownloadable((Boolean) material.get("is_downloadable"));
            
            result.add(vo);
        }
        
        return result;
    }
    

    /**
     * 删除课程资料
     * 语法逻辑：
     * 1. 查询资料信息
     * 2. 删除文件记录
     * 
     * 实现效果：
     * - 删除资料记录
     * - 权限控制由 Controller 层@RequiresPermissions 注解处理
     * 
     * @param materialId 资料 ID
     * @param userId 用户 ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteMaterial(Long materialId, Long userId) {
        // 1. 查询资料信息
        Map<String, Object> material = materialMapper.selectMaterialById(materialId);
        if (material == null) {
            throw new ServiceException("资料不存在");
        }
        
        // 2. 删除资料
        return materialMapper.deleteMaterialById(materialId);
    }
    
    // ==================== 课程问答接口实现 ====================
    
    /**
     * 提问（课程小节内）
     * 语法逻辑：
     * 1. 保存问题
     * 2. 关联课程和章节
     * 
     * 实现效果：
     * - 问题自动关联到课程和章节
     * - 状态默认为待回答
     * 
     * @param questionDTO 问题 DTO
     * @param userId 用户 ID
     * @return 问题 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long askQuestion(QuestionDTO questionDTO, Long userId) {
        // 1. 构建参数
        Map<String, Object> params = new HashMap<>();
        params.put("courseId", questionDTO.getCourseId());
        params.put("sectionId", questionDTO.getSectionId());
        params.put("userId", userId);
        params.put("questionTitle", questionDTO.getQuestionTitle());
        params.put("questionContent", questionDTO.getQuestionContent());
        
        // 2. 插入问题
        questionMapper.insertQuestion(params);
        
        // 3. 返回问题 ID
        return (Long) params.get("id");
    }

    /**
     * 回答问题
     * 语法逻辑：
     * 1. 查询问题信息
     * 2. 保存答案
     * 3. 更新状态
     * 
     * 实现效果：
     * - 状态更新为已回答
     * - 权限控制由 Controller 层@RequiresPermissions 注解处理
     * 
     * @param questionId 问题 ID
     * @param answerContent 回答内容
     * @param userId 用户 ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int answerQuestion(Long questionId, String answerContent, Long userId) {
        // 1. 查询问题信息
        OshCourseQuestion question = questionMapper.selectQuestionById(questionId);
        if (question == null) {
            throw new ServiceException("问题不存在");
        }
        
        // 2. 更新问题和答案
        Map<String, Object> params = new HashMap<>();
        params.put(CourseQuestionConstants.FIELD_ID, questionId);
        params.put(CourseQuestionConstants.FIELD_ANSWER_CONTENT, answerContent);
        params.put(CourseQuestionConstants.FIELD_ANSWER_USER_ID, userId);
        params.put(CourseQuestionConstants.FIELD_ANSWER_TIME, DateUtils.getNowDate());
        params.put(CourseQuestionConstants.FIELD_STATUS, CourseQuestionConstants.STATUS_ANSWERED);
        
        return questionMapper.updateQuestion(params);
    }
    
    /**
     * 跳转到问答板块问题详情
     * 语法逻辑：
     * 1. 查询问题详情
     * 2. 关联用户信息
     * 
     * 实现效果：
     * - 返回问题完整信息
     * 
     * @param questionId 问题 ID
     * @return 问题详情
     */
    @Override
    public QuestionDTO getQuestionDetail(Long questionId) {
        // 1. 查询问题详情
        OshCourseQuestion question = questionMapper.selectQuestionById(questionId);
        if (question == null) {
            throw new ServiceException("问题不存在");
        }
        
        // 2. 构建返回结果
        QuestionDTO dto = new QuestionDTO();
        dto.setId(questionId);
        dto.setCourseId(question.getCourseId().longValue());
        dto.setSectionId(question.getSectionId().longValue());
      //  dto.setQuestionTitle(question.getQuestionTitle());
       // dto.setQuestionContent(question.getQuestionContent( ));
        
        return dto;
    }
    
    // ==================== 课程评价接口实现 ====================
    
    /**
     * 提交课程评价
     * 语法逻辑：
     * 1. 检查是否已评价
     * 2. 保存评价
     * 
     * 实现效果：
     * - 好评/中评/差评三选一
     * - 每个用户对每门课程只能评价一次
     * 
     * @param reviewDTO 评价 DTO
     * @param userId 用户 ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int submitReview(ReviewDTO reviewDTO, Long userId) {
        // 1. 检查是否已评价（实际开发中需要查询）
        // List<Map<String, Object>> reviews = reviewMapper.selectReviewsByCourseId(reviewDTO.getCourseId());
        // for (Map<String, Object> review : reviews) {
        //     if (review.get("user_id").equals(userId)) {
        //         throw new ServiceException("您已经评价过该课程");
        //     }
        // }
        
        // 2. 保存评价
        Map<String, Object> params = new HashMap<>();
        params.put(CourseReviewConstants.FIELD_COURSE_ID, reviewDTO.getCourseId());
        params.put(CourseReviewConstants.FIELD_USER_ID, userId);
        params.put(CourseReviewConstants.FIELD_RATING, reviewDTO.getRating());
        params.put(CourseReviewConstants.FIELD_REVIEW_CONTENT, reviewDTO.getReviewContent());
        
        return reviewMapper.insertReview(params);
    }
    
    /**
     * 获取课程评价统计
     * 语法逻辑：
     * 1. 统计各评价数量
     * 2. 计算平均分
     * 
     * 实现效果：
     * - 返回好评、中评、差评数量
     * - 计算平均评分
     * 
     * @param courseId 课程 ID
     * @return 评价统计 Map
     */
    @Override
    public Map<String, Object> getReviewStatistics(Long courseId) {
        // 1. 查询评价统计
        Map<String, Object> stats = reviewMapper.countReviewsByCourseId(courseId);
        
        // 2. 构建返回结果
        Map<String, Object> result = new HashMap<>();
        if (stats != null) {
            result.put(CourseReviewConstants.FIELD_GOOD_COUNT, stats.getOrDefault(CourseReviewConstants.DB_FIELD_GOOD_COUNT, CourseReviewConstants.DEFAULT_COUNT));
            result.put(CourseReviewConstants.FIELD_MEDIUM_COUNT, stats.getOrDefault(CourseReviewConstants.DB_FIELD_MEDIUM_COUNT, CourseReviewConstants.DEFAULT_COUNT));
            result.put(CourseReviewConstants.FIELD_BAD_COUNT, stats.getOrDefault(CourseReviewConstants.DB_FIELD_BAD_COUNT, CourseReviewConstants.DEFAULT_COUNT));
            result.put(CourseReviewConstants.FIELD_TOTAL_COUNT, stats.getOrDefault(CourseReviewConstants.DB_FIELD_TOTAL_COUNT, CourseReviewConstants.DEFAULT_COUNT));
            result.put(CourseReviewConstants.FIELD_AVERAGE_RATING, stats.getOrDefault(CourseReviewConstants.DB_FIELD_AVERAGE_RATING, CourseReviewConstants.DEFAULT_AVERAGE_RATING));
        } else {
            result.put(CourseReviewConstants.FIELD_GOOD_COUNT, CourseReviewConstants.DEFAULT_COUNT);
            result.put(CourseReviewConstants.FIELD_MEDIUM_COUNT, CourseReviewConstants.DEFAULT_COUNT);
            result.put(CourseReviewConstants.FIELD_BAD_COUNT, CourseReviewConstants.DEFAULT_COUNT);
            result.put(CourseReviewConstants.FIELD_TOTAL_COUNT, CourseReviewConstants.DEFAULT_COUNT);
            result.put(CourseReviewConstants.FIELD_AVERAGE_RATING, CourseReviewConstants.DEFAULT_AVERAGE_RATING);
        }
        
        return result;
    }
    
    // ==================== 课程服务人员接口实现 ====================
    
    /**
     * 申请成为课程服务人员
     * 语法逻辑：
     * 1. 检查是否已申请
     * 2. 保存申请记录
     * 
     * 实现效果：
     * - 通过考试与审核的人可以申请
     * - 状态默认为待审核
     * 
     * @param courseId 课程 ID
     * @param staffType 服务类型
     * @param examScore 考试成绩
     * @param userId 用户 ID
     * @return 申请 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applyStaff(Long courseId, String staffType, Integer examScore, Long userId) {
        // 1. 检查是否已申请
        Map<String, Object> existingStaff = staffMapper.selectStaffByUserIdAndCourseId(userId, courseId);
        if (existingStaff != null) {
            String auditStatus = (String) existingStaff.get(CourseQuestionConstants.FIELD_AUDIT_STATUS);
            if (CourseQuestionConstants.AUDIT_STATUS_APPROVED.equals(auditStatus)) {
                throw new ServiceException("您已经是该课程的服务人员");
            } else if (CourseQuestionConstants.AUDIT_STATUS_PENDING.equals(auditStatus)) {
                throw new ServiceException("您的申请正在审核中");
            }
        }
        
        // 2. 保存申请记录
        Map<String, Object> params = new HashMap<>();
        params.put(CourseLearningConstants.FIELD_USER_ID, userId);
        params.put(CourseReviewConstants.FIELD_COURSE_ID, courseId);
        params.put("staffType", staffType);
        params.put("examScore", examScore);
        
        staffMapper.insertStaff(params);
        
        // 3. 返回申请 ID
        return (Long) params.get("id");
    }
    
    /**
     * 审核服务人员申请
     * 语法逻辑：
     * 1. 更新审核状态
     * 
     * 实现效果：
     * - 审核通过后成为服务人员
     * - 权限控制由 Controller 层@RequiresPermissions 注解处理
     * 
     * @param applyId 申请 ID
     * @param auditStatus 审核状态
     * @param auditRemark 审核备注
     * @param userId 审核人用户 ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int auditStaff(Long applyId, String auditStatus, String auditRemark, Long userId) {
        // 1. 更新审核状态
        Map<String, Object> params = new HashMap<>();
        params.put(CourseQuestionConstants.FIELD_ID, applyId);
        params.put(CourseQuestionConstants.FIELD_AUDIT_STATUS, auditStatus);
        params.put(CourseQuestionConstants.FIELD_AUDIT_USER_ID, userId);
        params.put(CourseQuestionConstants.FIELD_AUDIT_REMARK, auditRemark);
        
        return staffMapper.updateStaffAudit(params);
    }
    
    /**
     * 获取课程服务人员列表
     * 语法逻辑：
     * 1. 查询服务人员
     * 2. 关联用户信息
     * 
     * 实现效果：
     * - 返回课程所有服务人员
     * - 包含用户基本信息
     * 
     * @param courseId 课程 ID
     * @return 服务人员列表
     */
    @Override
    public List<Map<String, Object>> getCourseStaffs(Long courseId) {
        return staffMapper.selectStaffsByCourseId(courseId);
    }
    



    /**
     * 检查用户是否已购买课程
     * 语法逻辑：
     * 1. 查询学习进度表
     * 2. 查询订单表
     * 
     * 实现效果：
     * - 有学习进度记录表示已购买
     * - 有订单记录表示已购买
     * 
     * @param courseId 课程 ID
     * @param userId 用户 ID
     * @return 是否已购买
     */
    
    // ==================== 标签查询接口实现 ====================
    
    /**
     * 根据关键字模糊查询标签
     * 
     * @param keyword 关键字（可选）
     * @return 标签列表
     */
    @Override
    public List<Map<String, Object>> searchTags(String keyword) {
        // 无关键字时返回全部启用标签；有关键字时再走模糊查询（LIMIT 20）
        if (StringUtils.isBlank(keyword)) {
            return tagMapper.selectAllTags();
        }
        return tagMapper.selectTagsByKeyword(keyword);
    }
    
    /**
     * 新增标签
     * 
     * @param tag 标签信息
     * @param userId 用户ID
     * @return 标签ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addTag(OshCourseTag tag, Long userId) {
        // 1. 检查标签名称是否已存在
        if (tagMapper.checkTagNameExists(tag.getName()) > 0) {
            throw new ServiceException("标签名称已存在");
        }
        
        // 2. 设置默认值
        tag.setCreateBy(String.valueOf(userId));
        tag.setUpdateBy(String.valueOf(userId));
        tag.setCreateTime(DateUtils.getNowDate());
        tag.setUpdateTime(DateUtils.getNowDate());
        if (tag.getSort() == null) {
            tag.setSort(CourseTagConstants.DEFAULT_SORT);
        }
        if (tag.getStatus() == null) {
            tag.setStatus(CourseTagConstants.DEFAULT_STATUS_ENABLED);
        }
        if (tag.getUseCount() == null) {
            tag.setUseCount(CourseTagConstants.DEFAULT_USE_COUNT);
        }
        
        // 3. 插入标签
        int result = tagMapper.insertTag(tag);
        if (result <= 0) {
            throw new ServiceException("新增标签失败");
        }
        
        return tag.getId();
    }
    
    // ==================== 课程收藏接口实现 ====================
    
    /**
     * 添加课程收藏
     * 
     * @param courseId 课程 ID
     * @param userId 用户 ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addFavorite(Long courseId, Long userId) {
        // 1. 检查是否已收藏
        int count = favaMapper.countFava(userId, courseId, CourseQuestionConstants.FAVORITE_TYPE_COURSE);
        if (count > 0) {
            return 1; // 已收藏，直接返回成功
        }
        
        // 2. 保存收藏记录
        OshFava fava = new OshFava();
        fava.setUserId(userId);
        fava.setGoodsId(courseId);
        fava.setType(CourseQuestionConstants.FAVORITE_TYPE_COURSE);
        int result = favaMapper.insertFava(fava);
        
        // 注：收藏数由osh_fava表统计查询，不再维护osh_course表的冗余字段
        
        return result;
    }
    
    /**
     * 取消课程收藏
     * 
     * @param courseId 课程 ID
     * @param userId 用户 ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int removeFavorite(Long courseId, Long userId) {
        // 1. 删除收藏记录
        OshFava fava = new OshFava();
        fava.setUserId(userId);
        fava.setGoodsId(courseId);
        fava.setType(CourseQuestionConstants.FAVORITE_TYPE_COURSE);
        int result = favaMapper.deleteFava(fava);
        
        // 注：收藏数由osh_fava表统计查询，不再维护osh_course表的冗余字段
        
        return result;
    }
    
    /**
     * 检查用户是否已收藏课程
     * 
     * @param courseId 课程 ID
     * @param userId 用户 ID
     * @return 是否已收藏
     */
    @Override
    public boolean checkFavorited(Long courseId, Long userId) {
        if (userId == null) {
            return false;
        }
        return favaMapper.countFava(userId, courseId, "course") > 0;
    }
    

    
    /**
     * 新增课程、章节及相关资料
     * @param courseCreateDTO 课程创建 DTO
     * @param userId 用户 ID
     * @return 课程 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCourseWithSections(CourseCreateDTO courseCreateDTO, Long userId) {
        // 1. 校验必填参数 
        if (StringUtils.isEmpty(courseCreateDTO.getTitle())) {
            throw new ServiceException("课程标题不能为空");
        }
        // TODO 办：校验其他必填参数（如tagr、type、price 等）
        
        // 2. 保存课程信息
        OshCourse course =  new OshCourse();
        BeanUtils.copyProperties(courseCreateDTO, course);
        course.setCreateBy(String.valueOf(userId));
        course.setCreateTime(DateUtils.getNowDate());
        course.setUpdateTime(DateUtils.getNowDate());

        int result = courseMapper.insertCourse(course);
        if (result <= 0) {
            throw new ServiceException("新增课程失败");
        }

        Long courseId = course.getId();
        // 3. 保存章节信息
        int sectionCount = saveSectionsAndMaterials(courseId, courseCreateDTO.getSections());
        
        // 4. 更新课程章节数量
        if (sectionCount > 0) {
            Map<String, Object> params = new HashMap<>();
            params.put("id", courseId);
            params.put("subCount", sectionCount);
            courseMapper.updateCourseSubCount(params);
        }
        
        // 5. 保存标签关联关系
        saveCourseTags(courseId, courseCreateDTO.getTagIds());
        
        return courseId;
    }
    
    
    /**
     * 保存章节及资料信息
     * @return 章节数量
     */
    private int saveSectionsAndMaterials(Long courseId, List<SectionCreateDTO> sections) {
        if (sections == null || sections.isEmpty()) {
            return 0;
        }
        
        int sectionCount = 0;
        for (SectionCreateDTO sectionDTO : sections) {
            if (sectionDTO == null) {
                continue;
            }
            
            // 1. 构建章节实体
            OshCourseSection section = new OshCourseSection();
            BeanUtils.copyProperties(sectionDTO, section);
            section.setCourseId(courseId);
            
            // 设置默认值
            if (section.getParentId() == null) {
                section.setParentId(0L);
            }
            if (section.getSort() == null) {
                section.setSort(0);
            }
            if (section.getIsFree() == null) {
                section.setIsFree(0);
            }
            if (section.getStatus() == null) {
                section.setStatus(1);
            }

            // 2. 保存章节
            sectionMapper.insertSectionEntity(section);
            Long sectionId = section.getId();
            sectionCount++;
            
            // 3. 保存课时视频（作为特殊资料类型）
            if (StringUtils.isNotEmpty(sectionDTO.getMediaUrl())) {
                saveVideoMaterial(courseId, sectionId, sectionDTO);
            }
            
            // 4. 保存课件资料
            saveSectionMaterials(courseId, sectionId, sectionDTO.getMaterials());
        }
        
        return sectionCount;
    }
    
    /**
     * 保存课时视频资料
     * 视频作为特殊资料类型，关联到章节
     * 
     * @param courseId 课程ID
     * @param sectionId 章节ID
     * @param sectionDTO 章节DTO
     */
    private void saveVideoMaterial(Long courseId, Long sectionId, SectionCreateDTO sectionDTO) {
        OshCourseMaterial material = new OshCourseMaterial();
        material.setMaterialName(sectionDTO.getTitle() );
        material.setFileUrl(sectionDTO.getMediaUrl());
        material.setFileType("video");
        // 设置关联
        material.setCourseId(courseId);
        material.setSectionId(sectionId);
        // material.setIsPayOnly(sectionDTO.getIsFree() != null && sectionDTO.getIsFree() == 1 ? 0 : 1); // 表中无此字段
        material.setSort(0);
        
        materialMapper.insertMaterialEntity(material);
    }
    
    /**
     * 保存章节资料（课件压缩包）
     * 
     * @param courseId 课程ID
     * @param sectionId 章节ID
     * @param materials 资料列表
     */
    private void saveSectionMaterials(Long courseId, Long sectionId, List<SectionMaterialDTO> materials) {
        if (materials == null || materials.isEmpty()) {
            return;
        }
        
        // 允许的文件类型白名单
        Set<String> allowedFileTypes = new HashSet<>(Arrays.asList(
            "zip", "rar", "7z", "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt"
        ));
        
        for (SectionMaterialDTO materialDTO : materials) {
            if (materialDTO == null) {
                log.warn("跳过空资料对象");
                continue;
            }
            
            // 校验文件类型
            if (materialDTO.getFileType() != null && 
                !allowedFileTypes.contains(materialDTO.getFileType().toLowerCase())) {
                log.warn("资料文件类型不支持: {}, 跳过该资料", materialDTO.getFileType());
                continue;
            }
            
            OshCourseMaterial material = new OshCourseMaterial();
            // DTO字段映射到实体字段
            material.setMaterialName(materialDTO.getName());
            material.setFileUrl(materialDTO.getUrl());
            material.setFileSize(materialDTO.getFileSize());
            material.setFileType(materialDTO.getFileType());
            // 设置关联
            material.setCourseId(courseId);
            material.setSectionId(sectionId);
            // 设置默认值
            // material.setIsPayOnly(1); // 表中无此字段
            if (material.getSort() == null) {
                material.setSort(0);
            }
    
            materialMapper.insertMaterialEntity(material);
        }
    }
    
    /**
     * 保存课程标签关联
     */
    private void saveCourseTags(Long courseId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        
        for (Long tagId : tagIds) {
            Map<String, Object> params = new HashMap<>();
            params.put("courseId", courseId);
            params.put("tagId", tagId);
            tagMapper.insertCourseTagRelation(params);
            tagMapper.incrementUsageCount(tagId);
        }
    }
    
    
    // ==================== 添加章节/课时接口实现 ====================
    
    /**
     * 添加章节/课时
     * 语法逻辑：
     * 1. 校验课程是否存在
     * 2. 校验用户权限（课程创建者或服务人员）
     * 3. 保存章节信息
     * 4. 保存章节资料（可选）
     * 5. 更新课程章节数量
     * 
     * 实现效果：
     * - 为已存在的课程追加新的章节或课时内容
     * - 权限控制由 Controller 层 @RequiresPermissions 注解处理
     * 
     * @param courseId 课程 ID
     * @param sectionCreateDTO 章节创建 DTO
     * @param userId 用户 ID
     * @return 章节 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addSection(Long courseId, SectionCreateDTO sectionCreateDTO, Long userId) {
        // 1. 校验课程是否存在
        OshCourse course = courseMapper.selectCourseById(courseId);
        if (course == null) {
            throw new ServiceException("课程不存在");
        }
        
        // 校验必填参数
        if (StringUtils.isEmpty(sectionCreateDTO.getTitle())) {
            throw new ServiceException("章节标题不能为空");
        }

        OshCourseSection section = new OshCourseSection();
        BeanUtils.copyProperties(sectionCreateDTO, section);
        section.setCourseId(courseId);
        section.setStatus(1);

        
        sectionMapper.insertSectionEntity(section);
        Long sectionId = section.getId();
        
        // 4. 保存课时视频（作为特殊资料类型）
        if (StringUtils.isNotEmpty(sectionCreateDTO.getMediaUrl())) {
            saveVideoMaterial(courseId, sectionId, sectionCreateDTO);
        }
        
        // 5. 保存课件资料
        saveSectionMaterials(courseId, sectionId, sectionCreateDTO.getMaterials());
        
        // 6. 课程章节数量 +1
        Map<String, Object> params = new HashMap<>();
        params.put("id", courseId);
        int currentCount = course.getSubCount() != null ? course.getSubCount() : 0;
        params.put("subCount", currentCount + 1);
        courseMapper.updateCourseSubCount(params);
        
        return sectionId;
    }
    
    
    /**
     * 获取课程购买状态
     * 语法逻辑：检查学习进度→检查订单表→判断是否过期
     * 实现效果：返回用户对课程的购买状态获取上传文件的用户信息吗？
当前登录用户（已购买/未购买/已过期）
     *
     * @param courseId 课程 ID
     * @param userId 用户 ID（可为空，未登录时返回未购买）
     * @return 购买状态 VO
     */
    @Override
    public CoursePurchaseStatusVO getPurchaseStatus(Long courseId, Long userId) {
        CoursePurchaseStatusVO vo = new CoursePurchaseStatusVO();
        vo.setCourseId(courseId);
        
        // 1. 检查课程是否存在
        OshCourse course = courseMapper.selectCourseById(courseId);
        if (course == null) {
            vo.setIsPurchased(false);
            vo.setReason("课程不存在");
            return vo;
        }
        vo.setCourseName(course.getTitle());
        
        // 2. 未登录用户
        if (userId == null) {
            vo.setIsPurchased(false);
            vo.setReason("请先登录");
            return vo;
        }
        
        // 3. 检查学习进度（有进度表示已购买）
        Map<String, Object> progress = progressMapper.selectProgressByUserIdAndCourseId(userId, courseId);
        if (progress != null) {
            vo.setIsPurchased(true);
            vo.setIsExpired(false);
            vo.setPurchaseTime((Date) progress.get("create_time"));
            vo.setServiceType("永久有效");
            vo.setStatus("closed");
            return vo;
        }
        
        // 4. 检查订单表
        Map<String, Object> order = orderMapper.selectOrderByUserIdAndCourseId(userId, courseId);
        if (order == null) {
            vo.setIsPurchased(false);
            vo.setReason("未购买该课程");
            return vo;
        }
        
        // 5. 检查订单状态
        String status = (String) order.get("status");
        vo.setStatus(status);
        vo.setOrderNo((String) order.get("orderNo"));
        vo.setPrice(order.get("price") != null ? order.get("price").toString() : null);
        
        if (!"closed".equals(status)) {
            vo.setIsPurchased(false);
            vo.setReason("订单未支付");
            return vo;
        }
        
        // 6. 检查是否过期
        vo.setIsPurchased(true);
        vo.setPurchaseTime((Date) order.get("createdTime"));
        vo.setServiceType((String) order.get("serviceType"));
        
        Date expireTime = (Date) order.get("expireTime");
        vo.setExpireTime(expireTime);
        
        if (expireTime != null) {
            vo.setIsExpired(expireTime.before(new Date()));
            // 计算剩余天数
            if (!vo.getIsExpired()) {
                long days = (expireTime.getTime() - System.currentTimeMillis()) / (1000 * 60 * 60 * 24);
                vo.setRemainingDays(days);
            }
        } else {
            // 无过期时间表示永久有效
            vo.setIsExpired(false);
        }
        
        return vo;
    }
    
    /**
     * 检查用户是否已购买课程
     * 语法逻辑：检查订单表→判断是否过期
     *
     * @param courseId 课程 ID
     * @param userId 用户 ID
     * @return 是否已购买且未过期
     */
    @Override
    public boolean checkUserPurchased(Long courseId, Long userId) {
        if (userId == null) {
            return false;
        }

        // 1. 检查学习进度
        Map<String, Object> progress = progressMapper.selectProgressByUserIdAndCourseId(userId, courseId);
        if (progress != null) {
            return true;
        }

        // 2. 检查有效订单
        Map<String, Object> order = orderMapper.selectValidOrderByUserIdAndCourseId(userId, courseId);
        return order != null;
    }


    // ==================== 课程封面临时URL批量获取接口实现 ====================

    /**
     * 批量获取课程封面临时访问URL
     * 语法逻辑：根据课程ID列表查询封面相对路径，批量生成临时访问URL
     * 实现效果：返回课程ID到临时URL的映射，支持按需加载和懒加载
     *
     * @param courseIds 课程ID列表（最多支持50个）
     * @param minute 临时URL有效期（分钟）
     * @return 课程ID到临时URL的映射 Map<Long, String>
     */
    @Override
    public Map<Long, String> batchGetCourseCoverUrls(List<Long> courseIds, int minute) {
        Map<Long, String> result = new HashMap<>();

        if (courseIds == null || courseIds.isEmpty()) {
            return result;
        }

        // 限制最多50个，防止滥用
        int maxSize = Math.min(courseIds.size(), 50);
        List<Long> limitedIds = courseIds.subList(0, maxSize);

        // 1. 批量查询课程封面路径
        List<OshCourse> courses = courseMapper.selectCoursesByIds(limitedIds);
        if (courses == null || courses.isEmpty()) {
            return result;
        }

        // 2. 批量生成临时URL
        for (OshCourse course : courses) {
            if (course != null && StringUtils.isNotEmpty(course.getCover())) {
                try {
                    String signedUrl = ossService.getLimitedUrl(course.getCover(), minute);
                    result.put(course.getId(), signedUrl);
                } catch (Exception e) {
                    log.warn("生成课程封面临时URL失败, courseId={}, error={}", course.getId(), e.getMessage());
                    // 生成失败时返回空字符串，不影响其他封面
                    result.put(course.getId(), "");
                }
            } else {
                result.put(course.getId(), "");
            }
        }

        return result;
    }

    /**
     * 根据相对路径批量获取封面临时URL
     * 语法逻辑：直接根据封面相对路径列表生成临时访问URL
     * 实现效果：返回相对路径到临时URL的映射
     *
     * @param coverPaths 封面相对路径列表
     * @param minute 临时URL有效期（分钟）
     * @return 相对路径到临时URL的映射 Map<String, String>
     */
    @Override
    public Map<String, String> batchGetCoverUrlsByPaths(List<String> coverPaths, int minute) {
        Map<String, String> result = new HashMap<>();

        if (coverPaths == null || coverPaths.isEmpty()) {
            return result;
        }

        // 限制最多50个，防止滥用
        int maxSize = Math.min(coverPaths.size(), 50);
        for (int i = 0; i < maxSize; i++) {
            String path = coverPaths.get(i);
            if (StringUtils.isNotEmpty(path)) {
                try {
                    String signedUrl = ossService.getLimitedUrl(path, minute);
                    result.put(path, signedUrl);
                } catch (Exception e) {
                    log.warn("生成封面临时URL失败, path={}, error={}", path, e.getMessage());
                    result.put(path, "");
                }
            }
        }

        return result;
    }


    // ==================== 章节视频临时URL批量获取接口实现 ====================

    /**
     * 批量获取章节视频临时访问URL
     * 语法逻辑：根据章节ID列表查询media_url相对路径，批量生成临时访问URL
     * 实现效果：返回章节ID到临时URL的映射，支持按需加载
     *
     * @param sectionIds 章节ID列表（最多支持50个）
     * @param minute 临时URL有效期（分钟）
     * @return 章节ID到临时URL的映射 Map<Long, String>
     */
    @Override
    public Map<Long, String> batchGetSectionVideoUrls(List<Long> sectionIds, int minute) {
        Map<Long, String> result = new HashMap<>();

        if (sectionIds == null || sectionIds.isEmpty()) {
            return result;
        }

        // 限制最多50个，防止滥用
        int maxSize = Math.min(sectionIds.size(), 50);
        List<Long> limitedIds = sectionIds.subList(0, maxSize);

        // 1. 批量查询章节视频路径
        List<OshCourseSection> sections = sectionMapper.selectSectionsByIds(limitedIds);
        if (sections == null || sections.isEmpty()) {
            return result;
        }

        // 2. 批量生成临时URL
        for (OshCourseSection section : sections) {
            if (section != null && StringUtils.isNotEmpty(section.getMediaUrl())) {
                try {
                    String signedUrl = ossService.getLimitedUrl(section.getMediaUrl(), minute);
                    result.put(section.getId(), signedUrl);
                } catch (Exception e) {
                    log.warn("生成章节视频临时URL失败, sectionId={}, error={}", section.getId(), e.getMessage());
                    result.put(section.getId(), "");
                }
            } else {
                result.put(section.getId(), "");
            }
        }

        return result;
    }

    /**
     * 单个获取章节视频临时访问URL
     * 语法逻辑：根据章节ID查询media_url相对路径，生成临时访问URL
     * 实现效果：返回章节的临时访问URL
     *
     * @param sectionId 章节ID
     * @param minute 临时URL有效期（分钟）
     * @return 临时访问URL，未找到返回null
     */
    @Override
    public String getSectionVideoUrl(Long sectionId, int minute) {
        if (sectionId == null) {
            return null;
        }

        // 查询章节信息
        List<Long> ids = Collections.singletonList(sectionId);
        List<OshCourseSection> sections = sectionMapper.selectSectionsByIds(ids);

        if (sections == null || sections.isEmpty()) {
            log.warn("章节不存在, sectionId={}", sectionId);
            return null;
        }

        OshCourseSection section = sections.get(0);
        if (section == null || StringUtils.isEmpty(section.getMediaUrl())) {
            log.warn("章节视频路径为空, sectionId={}", sectionId);
            return null;
        }

        try {
            return ossService.getLimitedUrl(section.getMediaUrl(), minute);
        } catch (Exception e) {
            log.warn("生成章节视频临时URL失败, sectionId={}, error={}", sectionId, e.getMessage());
            return null;
        }
    }


    // ==================== 课程资料临时URL批量获取接口实现 ====================

    /**
     * 批量获取课程资料临时访问URL
     * 语法逻辑：根据资料ID列表查询url相对路径，批量生成临时访问URL
     * 实现效果：返回资料ID到临时URL的映射，支持按需加载
     *
     * @param materialIds 资料ID列表（最多支持50个）
     * @param minute 临时URL有效期（分钟）
     * @return 资料ID到临时URL的映射 Map<Long, String>
     */
    @Override
    public Map<Long, String> batchGetMaterialUrls(List<Long> materialIds, int minute) {
        Map<Long, String> result = new HashMap<>();

        if (materialIds == null || materialIds.isEmpty()) {
            return result;
        }

        // 限制最多50个，防止滥用
        int maxSize = Math.min(materialIds.size(), 50);
        List<Long> limitedIds = materialIds.subList(0, maxSize);

        // 1. 批量查询资料路径
        List<OshCourseMaterial> materials = materialMapper.selectMaterialsByIds(limitedIds);
        if (materials == null || materials.isEmpty()) {
            return result;
        }

        // 2. 批量生成临时URL
        for (OshCourseMaterial material : materials) {
            if (material != null && StringUtils.isNotEmpty(material.getUrl())) {
                try {
                    String signedUrl = ossService.getLimitedUrl(material.getUrl(), minute);
                    result.put(material.getId(), signedUrl);
                } catch (Exception e) {
                    log.warn("生成资料临时URL失败, materialId={}, error={}", material.getId(), e.getMessage());
                    result.put(material.getId(), "");
                }
            } else {
                result.put(material.getId(), "");
            }
        }

        return result;
    }

    /**
     * 单个获取课程资料临时访问URL
     * 语法逻辑：根据资料ID查询url相对路径，生成临时访问URL
     * 实现效果：返回资料的临时访问URL
     *
     * @param materialId 资料ID
     * @param minute 临时URL有效期（分钟）
     * @return 临时访问URL，未找到返回null
     */
    @Override
    public String getMaterialUrl(Long materialId, int minute) {
        if (materialId == null) {
            return null;
        }

        // 查询资料信息
        List<Long> ids = Collections.singletonList(materialId);
        List<OshCourseMaterial> materials = materialMapper.selectMaterialsByIds(ids);

        if (materials == null || materials.isEmpty()) {
            log.warn("资料不存在, materialId={}", materialId);
            return null;
        }

        OshCourseMaterial material = materials.get(0);
        if (material == null || StringUtils.isEmpty(material.getUrl())) {
            log.warn("资料URL为空, materialId={}", materialId);
            return null;
        }

        try {
            return ossService.getLimitedUrl(material.getUrl(), minute);
        } catch (Exception e) {
            log.warn("生成资料临时URL失败, materialId={}, error={}", materialId, e.getMessage());
            return null;
        }
    }

    @Override
    public Map<String, String> batchGetContentImageUrls(List<String> paths, int minute) {
        Map<String, String> result = new HashMap<>();
        if (paths == null || paths.isEmpty()) {
            return result;
        }
        // 获取 OSS 配置，用于从完整临时 URL 中提取 fileKey
        String bucketName = ossUtil.getOssProperties().getBucketName();
        for (String path : paths) {
            if (StringUtils.isEmpty(path)) {
                continue;
            }
            try {
                String fileKey = path;
                // 兼容完整临时 URL（旧数据存的是临时 URL）
                // 格式: https://<endpoint>/<bucketName>/<fileKey>?X-Amz-...
                if (path.startsWith("http")) {
                    String urlPath = path.contains("?") ? path.substring(0, path.indexOf("?")) : path;
                    String marker = "/" + bucketName + "/";
                    int idx = urlPath.indexOf(marker);
                    if (idx >= 0) {
                        fileKey = urlPath.substring(idx + marker.length());
                    }
                }
                // key 统一用 fileKey（相对路径），方便前端存到 data-src
                result.put(fileKey, ossService.getLimitedUrl(fileKey, minute));
            } catch (Exception e) {
                log.warn("生成内容图片临时URL失败, path={}, error={}", path, e.getMessage());
            }
        }
        return result;
    }
}


package com.backstage.system.service.course;

import com.backstage.common.core.page.TableDataInfo;
import com.backstage.system.domain.course.OshCourse;
import com.backstage.system.domain.course.OshCourseTag;
import com.backstage.system.domain.dto.*;
import com.backstage.system.domain.vo.*;
import com.backstage.system.domain.vo.CourseDetailVO;
import com.backstage.system.domain.vo.CourseMaterialVO;
import com.backstage.system.domain.vo.CourseSectionVO;
import com.backstage.system.domain.vo.SectionAccessVO;
import com.backstage.system.domain.vo.SectionProgressVO;
import com.backstage.system.domain.vo.SectionVideoVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import com.backstage.system.domain.vo.CoursePurchaseStatusVO;

/**
 * 课程管理 Service 接口
 * 
 * @author ruoyi
 * @date 2026-03-24
 */
public interface ICourseManageService {
    
    // ==================== 课程查询接口 ====================
    
    /**
     * 查询课程列表（带筛选和排序）
     * 语法逻辑：支持多标签筛选 + 关键字搜索，按标签使用量降序排列
     * 实现效果：返回分页课程列表，包含标签、评价统计等信息
     * 
     * @param courseDTO 查询条件 DTO
     * @return 分页课程列表
     */
    TableDataInfo selectCourseList(CourseQueryDTO courseDTO);
    



    /**
     * 获取课程标签列表（供前端多选下拉框使用）
     * 实现效果：返回所有启用标签，按课程使用数量（use_count）降序排列
     *
     * @return 标签列表（包含 id、name、useCount）
     */
    List<Map<String, Object>> selectTagList();
    
    /**
     * 获取课程详情
     * 语法逻辑：查询课程基本信息 + 章节列表 + 评价统计
     * 实现效果：返回课程完整信息，包括试看章节、价格、评价统计
     * 
     * @param courseId 课程 ID
     * @param userId 用户 ID（用于判断是否购买）
     * @return 课程详情 VO
     */
    CourseDetailVO getCourseDetail(Long courseId, Long userId);
    
    /**
     * 新增课程
     * 语法逻辑：校验用户权限→保存课程信息→关联标签
     * 实现效果：通过 system:course:add 权限控制访问，返回课程 ID
     * 
     * @param course 课程信息
     * @param userId 用户 ID
     * @return 结果
     */
    Long insertCourse(OshCourse course, Long userId);
    
    /**
     * 修改课程
     * 语法逻辑：校验用户权限→更新课程信息→更新标签关联
     * 实现效果：仅课程创建者或服务人员可修改
     * 
     * @param course 课程信息
     * @param userId 用户 ID
     * @return 结果
     */
    int updateCourse(OshCourse course, Long userId);
    
    /**
     * 删除课程
     * 语法逻辑：校验权限→级联删除章节、资料、问答等
     * 实现效果：物理删除课程及相关数据
     * 
     * @param courseId 课程 ID
     * @param userId 用户 ID
     * @return 结果
     */
    int deleteCourse(Long courseId, Long userId);
    
    
    // ==================== 课程章节接口 ====================
    
    /**
     * 获取课程大纲
     * 语法逻辑：查询所有章节→标记免费/付费状态→计算学习进度
     * 实现效果：显示所有章节，区分免费/付费、已学/未学状态
     * 
     * @param courseId 课程 ID
     * @param userId 用户 ID
     * @return 章节列表 VO
     */
    List<CourseSectionVO> getCourseSections(Long courseId, Long userId);
    
    /**
     * 立即学习（试看免费章节）
     * 语法逻辑：判断章节是否免费→检查是否已购买→返回学习内容
     * 实现效果：免费章节直接返回内容，付费章节提示购买
     * 
     * @param courseId 课程 ID
     * @param sectionId 章节 ID
     * @param userId 用户 ID
     * @return 章节内容
     */
    SectionDTO learnSection(Long courseId, Long sectionId, Long userId);
    
    /**
     * 更新学习进度
     * 语法逻辑：更新进度表→计算百分比→标记完成状态
     * 实现效果：记录用户学习的每个章节，计算整体学习进度
     * 
     * @param courseId 课程 ID
     * @param sectionId 章节 ID
     * @param userId 用户 ID
     * @param progress 学习进度百分比
     */
    void updateProgress(Long courseId, Long sectionId, Long userId, Double progress);
    
    
    // ==================== 视频播放接口 ====================
    
    /**
     * 获取章节视频信息
     * 语法逻辑：查询章节详情→检查访问权限→查询学习进度
     * 实现效果：返回视频URL、时长、封面、分辨率等信息，包含当前播放进度
     * 
     * @param sectionId 章节ID
     * @param userId 用户ID（可为空，未登录用户只能看免费章节）
     * @return 章节视频信息 VO
     */
    SectionVideoVO getSectionVideo(Long sectionId, Long userId);
    
    /**
     * 更新播放进度
     * 语法逻辑：更新章节学习进度表→累加学习时长→更新播放位置
     * 实现效果：记录用户视频播放进度，支持断点续播
     * 
     * @param sectionId 章节ID
     * @param progressDTO 进度更新请求
     * @param userId 用户ID
     */
    void updatePlayProgress(Long sectionId, ProgressDTO progressDTO, Long userId);
    
    /**
     * 获取播放历史
     * 语法逻辑：查询用户章节学习进度记录
     * 实现效果：返回上次播放位置、进度百分比、学习状态等
     * 
     * @param sectionId 章节ID
     * @param userId 用户ID
     * @return 章节学习进度 VO
     */
    SectionProgressVO getPlayProgress(Long sectionId, Long userId);
    
    /**
     * 记录观看完成
     * 语法逻辑：更新章节学习状态为已完成→更新课程整体进度
     * 实现效果：标记章节已学完，更新完成时间，触发考试跳转（如有）
     * 
     * @param sectionId 章节ID
     * @param userId 用户ID
     * @return 是否触发考试（返回考试ID则需跳转答题）
     */
    Long markSectionComplete(Long sectionId, Long userId);
    
    /**
     * 检查购买权限
     * 语法逻辑：检查章节是否免费→检查用户是否已购买课程
     * 实现效果：返回用户是否有权限观看此章节
     * 
     * @param sectionId 章节ID
     * @param userId 用户ID（可为空）
     * @return 章节访问权限 VO
     */
    SectionAccessVO checkSectionAccess(Long sectionId, Long userId);
    
    
    // ==================== 课程资料接口 ====================
    
    /**
     * 获取课程资料列表
     * 语法逻辑：检查购买状态→返回可下载资料
     * 实现效果：购买后显示可下载资料
     * 
     * @param courseId 课程 ID
     * @param userId 用户 ID
     * @return 资料列表 VO
     */
    List<CourseMaterialVO> getCourseMaterials(Long courseId, Long userId);
    

    /**
     * 删除课程资料
     * 语法逻辑：校验权限→删除文件记录
     * 实现效果：仅课程创建者或服务人员可删除
     * 
     * @param materialId 资料 ID
     * @param userId 用户 ID
     * @return 结果
     */
    int deleteMaterial(Long materialId, Long userId);
    
    
    // ==================== 课程问答接口 ====================
    
    /**
     * 提问（课程小节内）
     * 语法逻辑：保存问题→关联课程和章节
     * 实现效果：问题自动关联到课程和章节
     * 
     * @param questionDTO 问题 DTO
     * @param userId 用户 ID
     * @return 问题 ID
     */
    Long askQuestion(QuestionDTO questionDTO, Long userId);
    
    /**
     * 回答问题（仅课程服务人员）
     * 语法逻辑：校验服务人员身份→保存答案→更新状态
     * 实现效果：仅课程服务人员可回答
     * 
     * @param questionId 问题 ID
     * @param answerContent 回答内容
     * @param userId 用户 ID
     * @return 结果
     */
    int answerQuestion(Long questionId, String answerContent, Long userId);
    
    /**
     * 跳转到问答板块问题详情
     * 语法逻辑：查询问题详情→关联用户信息
     * 实现效果：返回问题完整信息
     * 
     * @param questionId 问题 ID
     * @return 问题详情
     */
    QuestionDTO getQuestionDetail(Long questionId);
    
    
    // ==================== 课程评价接口 ====================
    
    /**
     * 提交课程评价
     * 语法逻辑：检查是否已评价→保存评价→更新统计
     * 实现效果：好评/中评/差评三选一
     * 
     * @param reviewDTO 评价 DTO
     * @param userId 用户 ID
     * @return 结果
     */
    int submitReview(ReviewDTO reviewDTO, Long userId);
    
    /**
     * 获取课程评价统计
     * 语法逻辑：统计各评价数量→计算平均分
     * 实现效果：返回好评、中评、差评数量和平均分
     * 
     * @param courseId 课程 ID
     * @return 评价统计 Map
     */
    Map<String, Object> getReviewStatistics(Long courseId);
    
    
    // ==================== 课程服务人员接口 ====================
    
    /**
     * 申请成为课程服务人员
     * 语法逻辑：检查是否已申请→保存申请记录
     * 实现效果：通过考试与审核的人可以申请
     * 
     * @param courseId 课程 ID
     * @param staffType 服务类型
     * @param examScore 考试成绩
     * @param userId 用户 ID
     * @return 申请 ID
     */
    Long applyStaff(Long courseId, String staffType, Integer examScore, Long userId);
    
    /**
     * 审核服务人员申请（管理员）
     * 语法逻辑：校验管理员权限→更新审核状态
     * 实现效果：管理员审核申请
     * 
     * @param applyId 申请 ID
     * @param auditStatus 审核状态
     * @param auditRemark 审核备注
     * @param userId 审核人用户 ID
     * @return 结果
     */
    int auditStaff(Long applyId, String auditStatus, String auditRemark, Long userId);
    
    /**
     * 获取课程服务人员列表
     * 语法逻辑：查询服务人员→关联用户信息
     * 实现效果：返回课程所有服务人员
     * 
     * @param courseId 课程 ID
     * @return 服务人员列表
     */
    List<Map<String, Object>> getCourseStaffs(Long courseId);
    
    
    // ==================== 标签查询接口 ====================
    
    /**
     * 根据关键字模糊查询标签,按使用数量降序排列
     * 
     * @param keyword 关键字（可选）
     * @return 标签列表（包含 id、name、useCount）
     */
    List<Map<String, Object>> searchTags(String keyword);
    
    /**
     * 新增标签
     * 语法逻辑：检查名称是否已存在→保存标签信息
     * 实现效果：返回新标签ID
     * 
     * @param tag 标签信息
     * @param userId 用户ID
     * @return 标签ID
     */
    Long addTag(OshCourseTag tag, Long userId);
    
    
    // ==================== 课程收藏接口 ====================
    
    /**
     * 添加课程收藏
     * 语法逻辑：检查是否已收藏→保存收藏记录→更新收藏计数
     * 实现效果：用户收藏课程
     * 
     * @param courseId 课程 ID
     * @param userId 用户 ID
     * @return 结果
     */
    int addFavorite(Long courseId, Long userId);
    
    /**
     * 取消课程收藏
     * 语法逻辑：删除收藏记录→更新收藏计数
     * 实现效果：用户取消收藏
     * 
     * @param courseId 课程 ID
     * @param userId 用户 ID
     * @return 结果
     */
    int removeFavorite(Long courseId, Long userId);
    
    /**
     * 检查用户是否已收藏课程
     * 
     * @param courseId 课程 ID
     * @param userId 用户 ID
     * @return 是否已收藏
     */
    boolean checkFavorited(Long courseId, Long userId);
    
    
    // ==================== 视频上传接口 ====================
    
    /**
     * 上传课程封面图片
     * 语法逻辑：校验文件→调用上传接口→返回封面信息
     * 实现效果：将封面图片上传到文件服务器，返回封面信息
     *

     * @param file     封面文件
     * @return
     */
    Map<String, Object> uploadCourseCover(MultipartFile file, String coverName);
    

    
    /**
     * 上传视频
     * 语法逻辑：校验文件格式→上传视频→返回视频信息
     * 实现效果：支持 mp4/avi/mov/mkv/flv 格式，存储到指定目录
     *
     * @param file 视频文件
     * @param videoName 视频名称
     * @param sectionId 小节 ID（重新上传时传入，用于删除 OSS 旧视频）
     * @return 视频信息（名称、URL、大小、类型）
     */
    Map<String, Object> uploadVideo(MultipartFile file, String videoName, Long sectionId);
    
    /**
     * 上传课时资料
     * 语法逻辑：校验文件格式→上传压缩包→返回资料信息
     * 实现效果：支持 zip/rar/tar/gz 格式，存储到指定目录
     *
     * @param file 资料文件
     * @param materialName 资料名称
     * @return 资料信息（名称、URL、大小、类型）
     */
    Map<String, Object> uploadMaterial(MultipartFile file, String materialName);
    
    
    // ==================== 课程新增（含章节）接口 ====================
    
    /**
     * 新增课程（含章节）
     * 语法逻辑：校验参数→保存课程→保存章节→保存资料→更新标签关联
     * 实现效果：一次性保存课程基本信息、章节结构、课时内容及相关资料，使用事务确保数据一致性
     * 
     * @param courseCreateDTO 课程创建 DTO
     * @param userId 用户 ID
     * @return 课程 ID
     */
    Long createCourseWithSections(CourseCreateDTO courseCreateDTO, Long userId);
    
    
    // ==================== 添加章节/课时接口 ====================
    
    /**
     * 添加章节/课时
     * 语法逻辑：校验课程存在→保存章节→保存资料→更新课程章节数量
     * 实现效果：为已存在的课程追加新的章节或课时内容
     * 
     * @param courseId 课程 ID
     * @param sectionCreateDTO 章节创建 DTO
     * @param userId 用户 ID
     * @return 章节 ID
     */
    Long addSection(Long courseId, SectionCreateDTO sectionCreateDTO, Long userId);
    
    
    // ==================== 购买状态查询接口 ====================
    
    /**
     * 获取课程购买状态
     * 语法逻辑：检查学习进度→检查订单表→判断是否过期
     * 实现效果：返回用户对课程的购买状态（已购买/未购买/已过期）
     * 
     * @param courseId 课程 ID
     * @param userId 用户 ID（可为空，未登录时返回未购买）
     * @return 购买状态 VO
     */
    CoursePurchaseStatusVO getPurchaseStatus(Long courseId, Long userId);
    
    /**
     * 检查用户是否已购买课程
     * 语法逻辑：检查学习进度→检查订单表→判断是否过期
     * 实现效果：返回布尔值，用于快速判断
     * 
     * @param courseId 课程 ID
     * @param userId 用户 ID
     * @return 是否已购买且未过期
     */
    boolean checkUserPurchased(Long courseId, Long userId);
    
    
    // ==================== 课程封面临时URL批量获取接口 ====================
    
    /**
     * 批量获取课程封面临时访问URL
     * 语法逻辑：根据课程ID列表查询封面相对路径，批量生成临时访问URL
     * 实现效果：返回课程ID到临时URL的映射，支持按需加载和懒加载
     * 适用场景：课程列表页、推荐位等需要批量展示封面的场景
     * 
     * @param courseIds 课程ID列表（最多支持50个，超出部分忽略）
     * @param minute 临时URL有效期（分钟），建议值：30分钟，默认30
     * @return 课程ID到临时URL的映射 Map<Long, String>
     */
    Map<Long, String> batchGetCourseCoverUrls(List<Long> courseIds, int minute);
    
    /**
     * 根据相对路径批量获取封面临时URL
     * 语法逻辑：直接根据封面相对路径列表生成临时访问URL
     * 实现效果：返回相对路径到临时URL的映射
     * 
     * @param coverPaths 封面相对路径列表（如：common/image/course/cover/202604/xxx.jpg）
     * @param minute 临时URL有效期（分钟）
     * @return 相对路径到临时URL的映射 Map<String, String>
     */
    Map<String, String> batchGetCoverUrlsByPaths(List<String> coverPaths, int minute);
    
    
    // ==================== 章节视频临时URL批量获取接口 ====================
    
    /**
     * 批量获取章节视频临时访问URL
     * 语法逻辑：根据章节ID列表查询media_url相对路径，批量生成临时访问URL
     * 实现效果：返回章节ID到临时URL的映射，支持按需加载
     * 适用场景：视频播放器加载、批量预加载视频URL等
     * 
     * @param sectionIds 章节ID列表（最多支持50个，超出部分忽略）
     * @param minute 临时URL有效期（分钟），建议值：60分钟，默认60
     * @return 章节ID到临时URL的映射 Map<Long, String>
     */
    Map<Long, String> batchGetSectionVideoUrls(List<Long> sectionIds, int minute);
    
    /**
     * 单个获取章节视频临时访问URL
     * 语法逻辑：根据章节ID查询media_url相对路径，生成临时访问URL
     * 实现效果：返回章节的临时访问URL
     * 
     * @param sectionId 章节ID
     * @param minute 临时URL有效期（分钟）
     * @return 临时访问URL，未找到返回null
     */
    String getSectionVideoUrl(Long sectionId, int minute);
    
    
    // ==================== 课程资料临时URL批量获取接口 ====================
    
    /**
     * 批量获取课程资料临时访问URL
     * 语法逻辑：根据资料ID列表查询url相对路径，批量生成临时访问URL
     * 实现效果：返回资料ID到临时URL的映射，支持按需加载
     * 适用场景：资料下载、批量预加载资料URL等
     * 
     * @param materialIds 资料ID列表（最多支持50个，超出部分忽略）
     * @param minute 临时URL有效期（分钟），建议值：120分钟，默认120
     * @return 资料ID到临时URL的映射 Map<Long, String>
     */
    Map<Long, String> batchGetMaterialUrls(List<Long> materialIds, int minute);
    
    /**
     * 单个获取课程资料临时访问URL
     * 语法逻辑：根据资料ID查询url相对路径，生成临时访问URL
     * 实现效果：返回资料的临时访问URL
     * 
     * @param materialId 资料ID
     * @param minute 临时URL有效期（分钟）
     * @return 临时访问URL，未找到返回null
     */
    String getMaterialUrl(Long materialId, int minute);

    /**
     * 批量获取课程内容图片临时访问URL
     * 用于 textContent 富文本中图片的临时 URL 刷新
     * 接收相对路径列表，返回 相对路径 -> 临时URL 的映射
     *
     * @param relativePaths 图片相对路径列表（最多50个）
     * @param minute 临时URL有效期（分钟）
     * @return 相对路径到临时URL的映射 Map<String, String>
     */
    Map<String, String> batchGetContentImageUrls(List<String> relativePaths, int minute);
}
package com.backstage.system.mapper.course;

import com.backstage.system.domain.course.OshCourseCollection;
import com.backstage.system.domain.course.OshCourseSection;
import com.backstage.system.domain.course.OshCourseMaterial;
import com.backstage.system.domain.course.vo.CourseSearchLoginVo;
import com.backstage.system.domain.course.vo.OshCourseDetailVo;
import com.backstage.system.domain.course.vo.OshCourseSectionVo;
import com.backstage.system.domain.course.OshCourse;
import com.backstage.system.request.CourseSearchRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

/**
 * 课程信息 Mapper 接口
 * 
 * @author ruoyi
 * @date 2026-01-XX
 */
@Mapper
public interface OshCourseMapper
{
    List<CourseSearchLoginVo> pageQuerySearchCourse(
            @Param("request") CourseSearchRequest request,
            @Param("userId") Long userId
    );

    List<CourseSearchLoginVo> pageQueryAllCoursesForEsSync(@Param("request") CourseSearchRequest request);

    List<Long> selectUserBoughtCourseIds(@Param("userId") Long userId, @Param("courseIds") List<Long> courseIds);

    List<CourseSearchLoginVo> pageQueryUserCollectionSearchCourse(@Param("userId") Long userId, @Param("request") CourseSearchRequest request);

    List<OshCourse> pageQueryUserCollectionCourse(@Param("userId") Long userId, @Param("request") CourseSearchRequest request);

    /**
     * 查询课程信息
     *
     * @param id 课程 ID
     * @return 课程信息
     */
    OshCourse selectCourseById(Long id);



    /**
     * 查询课程列表
     *
     * @return 课程列表
     */
    List<OshCourse> selectCourseList();

    /**
     * 根据条件查询课程列表（支持多标签筛选 + 关键字搜索 + 动态排序）
     *
     * @param params 查询参数：tagIds(List), keyword(String), sortBy(String), sortOrder(String)
     * @return 课程列表
     */
    List<OshCourse> selectCourseListByCondition(Map<String, Object> params);

    /**
     * 新增课程
     *
     * @param course 课程信息
     * @return 结果
     */
    int insertCourse(OshCourse course);

    /**
     * 修改课程信息
     *
     * @param course 课程信息
     * @return 结果
     */
    int updateCourse(OshCourse course);

    /**
     * 删除课程
     *
     * @param id 课程 ID
     * @return 结果
     */
    int deleteCourseById(Long id);

    /**
     * 批量删除课程
     *
     * @param ids 需要删除的数据 ID
     * @return 结果
     */
    int deleteCourseByIds(Long[] ids);

    OshCourseDetailVo getCourseDetail(@Param("id") Long id, @Param("userId") Long userId,
                                      @Param("includeUnpublished") boolean includeUnpublished);

    Integer isUserBuyCourseOrFreeCourse(Long courseId, Long userId);

    Integer countUserBoughtCourse(@Param("courseId") Long courseId, @Param("userId") Long userId);

    Integer countFreeCourse(@Param("courseId") Long courseId);

    Integer countFreeSectionInCourse(@Param("courseId") Long courseId, @Param("sectionId") Long sectionId);

    int increaseQuestionCount(@Param("courseId") Long courseId);

    int increaseCollectionCount(@Param("courseId") Long courseId);

    int decreaseCollectionCount(@Param("courseId") Long courseId);

    List<OshCourseSectionVo> selectCourseSectionList(@Param("courseId") Long courseId);

    Integer countCourseSectionInCourse(@Param("courseId") Long courseId, @Param("sectionId") Long sectionId);

    OshCourseSection selectCourseSectionById(@Param("id") Long id);

    int insertCourseSection(OshCourseSection section);
    int updateCourseSection(OshCourseSection section);

    /** 仅更新某节点的 parent_id 与 sort（拖拽排序专用，不影响其它字段） */
    int updateSectionOrder(@Param("id") Long id,
                           @Param("courseId") Long courseId,
                           @Param("parentId") Long parentId,
                           @Param("sort") Integer sort,
                           @Param("updateBy") String updateBy);


    String getCourseSectionContent(@Param("sectionId") Long sectionId);

    /**
     * 删除小节（根据ID逻辑删除单个小节）
     */
    int deleteCourseSectionById(@Param("id") Long id, @Param("updateBy") String updateBy);

    /**
     * 删除章节下的所有小节（用于删除章时的级联操作）
     */
    int deleteCourseSectionsByParentId(@Param("parentId") Long parentId, @Param("updateBy") String updateBy);

    List<OshCourseMaterial> getCourseMaterials(Long courseId);

    /**
     * 更新课程章节数统计
     *
     * @param params 包含courseId等参数的Map
     * @return 更新结果
     */
    int updateCourseSubCount(Map<String, Object> params);
    
    /**
     * 根据课程ID列表批量查询课程信息（仅查询id和cover字段）
     *
     * @param ids 课程ID列表
     * @return 课程列表（仅包含id和cover）
     */
    List<OshCourse> selectCoursesByIds(List<Long> ids);


    int deleteSectionsByCourseId(@Param("courseId") Long courseId, @Param("updateBy") String updateBy);
}

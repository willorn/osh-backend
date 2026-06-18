package com.backstage.system.service.impl;

import com.backstage.common.enums.ResourceCodePrefixEnum;
import com.backstage.common.utils.generate.GenerateUtil;
import com.backstage.system.domain.course.OshCourse;
import com.backstage.system.mapper.course.OshCourseMapper;
import com.backstage.system.service.ISysCourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 课程信息 Service 业务层处理
 * 
 * @author ruoyi
 * @date 2026-01-XX
 */
@Service
public class SysCourseServiceImpl implements ISysCourseService 
{
    @Autowired
    private OshCourseMapper sysCourseMapper;

    /**
     * 查询课程详情
     * 
     * @param id 课程 ID
     * @return 课程信息
     */
    @Override
    public OshCourse selectCourseById(Long id)
    {
        return sysCourseMapper.selectCourseById(id);
    }

    /**
     * 查询课程列表
     *
     * @return 课程集合
     */
    @Override
    public List<OshCourse> selectCourseList()
    {
        return sysCourseMapper.selectCourseList();
    }

    /**
     * 新增课程
     * 
     * @param course 课程信息
     * @return 结果
     */
    @Override
    public int insertCourse(OshCourse course)
    {
        if (course != null && (course.getNo() == null || course.getNo().isEmpty())) {
            course.setNo(GenerateUtil.generateResourceCode(ResourceCodePrefixEnum.COURSE));
        }
        return sysCourseMapper.insertCourse(course);
    }

    /**
     * 修改课程
     * 
     * @param course 课程信息
     * @return 结果
     */
    @Override
    public int updateCourse(OshCourse course)
    {
        return sysCourseMapper.updateCourse(course);
    }

    /**
     * 批量删除课程
     * 
     * @param ids 需要删除的课程 ID
     * @return 结果
     */
    @Override
    public int deleteCourseByIds(Long[] ids)
    {
        return sysCourseMapper.deleteCourseByIds(ids);
    }

    /**
     * 删除课程
     * 
     * @param id 课程 ID
     * @return 结果
     */
    @Override
    public int deleteCourseById(Long id)
    {
        return sysCourseMapper.deleteCourseById(id);
    }
}

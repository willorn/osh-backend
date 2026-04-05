package com.backstage.system.mapper.openSourceProject;


import com.backstage.system.domain.openSourceProject.OpenSourceProject;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;


@Mapper
public interface OpenSourceProjectMapper {


    /**
     * 根据名字查询详情
     *
     */
    OpenSourceProject selectOpenSourceProjectByName(String projectName);

    /**
     * 分页/条件查询列表
     */

    List<OpenSourceProject> selectOpenSourceProjectList(OpenSourceProject openSourceProject);

    /**
     * 根据ID查询
     */
    OpenSourceProject selectOpenSourceProjectById(Long id);

    /**
     * 新增开源项目
     * @param openSourceProject 开源项目信息
     */
    int add(OpenSourceProject openSourceProject);

    /**
     * 修改开源项目
     * @param openSourceProject 开源项目信息
     *  @return 结果
     */
    int updateOpenSourceProject(OpenSourceProject openSourceProject);


    /**
     * 下架开源项目
     */
    int deleteOpenSourceProjectById(Long id);

}

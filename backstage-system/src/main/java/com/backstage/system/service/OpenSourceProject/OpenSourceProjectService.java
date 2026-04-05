package com.backstage.system.service.OpenSourceProject;


import com.backstage.system.domain.openSourceProject.OpenSourceProject;

import java.util.List;

public interface OpenSourceProjectService {

    int add(OpenSourceProject openSourceProject);

    int updateOpenSourceProject(OpenSourceProject openSourceProject);

    /**
     * 下架开源项目（逻辑删除）
     */
    int deleteOpenSourceProjectById(Long id);

    /**
     * 根据名称查询开源项目详情
     */
    OpenSourceProject selectOpenSourceProjectByName(String projectName);

    /**
     * 分页查询开源项目列表
     */
    List<OpenSourceProject> selectOpenSourceProjectList(OpenSourceProject openSourceProject);

    /**
     * 根据ID查询开源项目详情
     */
    OpenSourceProject selectOpenSourceProjectById(Long id);

}

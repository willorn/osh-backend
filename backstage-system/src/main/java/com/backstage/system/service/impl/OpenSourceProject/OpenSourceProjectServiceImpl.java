package com.backstage.system.service.impl.OpenSourceProject;

import com.backstage.common.utils.DateUtils;
import com.backstage.system.domain.openSourceProject.OpenSourceProject;
import com.backstage.system.mapper.openSourceProject.OpenSourceProjectMapper;
import com.backstage.system.service.OpenSourceProject.OpenSourceProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class OpenSourceProjectServiceImpl implements OpenSourceProjectService {

    @Autowired
    private OpenSourceProjectMapper openSourceProjectMapper;


    /**
     * 新增开源项目
     * @param openSourceProject
     * @return
     */
    @Override
    public int add(OpenSourceProject openSourceProject) {
        return openSourceProjectMapper.add(openSourceProject);
    }

    /**
     * 更新开源项目
     * @param openSourceProject
     * @return
     */
    @Override
    public int updateOpenSourceProject(OpenSourceProject openSourceProject) {


        openSourceProject.setUpdateTime(DateUtils.getNowDate());
        return openSourceProjectMapper.updateOpenSourceProject(openSourceProject);
    }

    /**
     * 下架开源项目
     * @param id
     * @return
     */
    @Override
    public int deleteOpenSourceProjectById(Long id) {
        return openSourceProjectMapper.deleteOpenSourceProjectById(id);
    }

    /**
     * 根据名字查找开源项目
     * @param projectName
     * @return
     */
    @Override
    public OpenSourceProject selectOpenSourceProjectByName(String projectName) {
        return openSourceProjectMapper.selectOpenSourceProjectByName(projectName);
    }

    /**
     * 查找开源项目列表
     * @param openSourceProject
     * @return
     */
    @Override
    public List<OpenSourceProject> selectOpenSourceProjectList(OpenSourceProject openSourceProject) {
        return openSourceProjectMapper.selectOpenSourceProjectList(openSourceProject);
    }

    /**
     * 根据id查找开源项目
     * @param id
     * @return
     */
    @Override
    public OpenSourceProject selectOpenSourceProjectById(Long id) {
        return openSourceProjectMapper.selectOpenSourceProjectById(id);
    }
}

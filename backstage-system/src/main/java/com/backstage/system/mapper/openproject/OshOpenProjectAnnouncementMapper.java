package com.backstage.system.mapper.openproject;

import com.backstage.system.domain.vo.tool.ToolAnnouncementVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OshOpenProjectAnnouncementMapper {

    @Select("SELECT id, CONCAT('最新同步开源项目：', project_name) AS title, project_url AS link, last_sync_time AS createTime " +
            "FROM osh_open_project " +
            "WHERE delete_flag = 0 AND status = 1 AND last_sync_time IS NOT NULL " +
            "ORDER BY last_sync_time DESC, id DESC LIMIT 1")
    List<ToolAnnouncementVO> selectLatestSyncedProjectAnnouncement();

    @Select("SELECT id, CONCAT('新配置 GitHub 数据源：', source_name, '（', github_owner, '）') AS title, github_url AS link, create_time AS createTime " +
            "FROM osh_open_project_source " +
            "WHERE delete_flag = 0 " +
            "ORDER BY create_time DESC, id DESC LIMIT 1")
    List<ToolAnnouncementVO> selectLatestSourceAnnouncement();
}

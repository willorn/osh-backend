package com.backstage.system.mapper.openproject;

import com.backstage.system.domain.vo.tool.ToolAnnouncementVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OshOpenProjectAnnouncementMapper {

    @Select("SELECT id, title, link, create_time AS createTime " +
            "FROM osh_announcement " +
            "WHERE delete_flag = 0 AND status = 4 AND source_module = 'openproject' " +
            "ORDER BY create_time DESC, id DESC LIMIT #{limit}")
    List<ToolAnnouncementVO> selectLatestAnnouncements(@Param("limit") int limit);

    @Select("SELECT id, title, link, create_time AS createTime " +
            "FROM osh_announcement " +
            "WHERE delete_flag = 0 AND status = 4 AND source_module = 'openproject' AND channel = #{channel} " +
            "ORDER BY sort DESC, create_time DESC, id DESC LIMIT #{limit}")
    List<ToolAnnouncementVO> selectLatestByChannel(@Param("channel") int channel, @Param("limit") int limit);

    @Select("SELECT COUNT(1) FROM osh_announcement " +
            "WHERE delete_flag = 0 AND source_module = 'openproject' AND channel = #{channel} " +
            "AND resource_type = #{resourceType} AND resource_id = #{resourceId}")
    int countByResource(@Param("channel") int channel,
                        @Param("resourceType") String resourceType,
                        @Param("resourceId") Long resourceId);

    @Insert("INSERT INTO osh_announcement " +
            "(title, link, icon_code, channel, resource_type, resource_id, sort, status, source, source_module, delete_flag, create_by, create_time, update_by, update_time) " +
            "VALUES " +
            "(#{title}, #{link}, #{iconCode}, #{channel}, #{resourceType}, #{resourceId}, #{sort}, 4, 'system', 'openproject', 0, 'system', NOW(), 'system', NOW())")
    int insertOpenProjectAnnouncement(@Param("title") String title,
                                      @Param("link") String link,
                                      @Param("iconCode") String iconCode,
                                      @Param("channel") int channel,
                                      @Param("resourceType") String resourceType,
                                      @Param("resourceId") Long resourceId,
                                      @Param("sort") int sort);
}

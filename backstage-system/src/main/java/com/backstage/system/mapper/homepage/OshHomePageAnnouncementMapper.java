package com.backstage.system.mapper.homepage;

import com.backstage.system.domain.announcement.vo.AnnouncementMarqueeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 首页公告查询 Mapper。
 */
@Mapper
public interface OshHomePageAnnouncementMapper {

    /**
     * 按资源类型和 channel 查询首页公告。
     */
    List<AnnouncementMarqueeVO> selectByResourceTypeAndChannel(@Param("resourceType") String resourceType,
                                                               @Param("channel") int channel,
                                                               @Param("limit") int limit);
}

package com.backstage.system.mapper.homepage;

import com.backstage.system.domain.announcement.vo.AnnouncementMarqueeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 首页公告栏 Mapper
 *
 * @author jayTatum
 */
@Mapper
public interface OshHomePageAnnouncementMapper {

    /**
     * 查询首页公告栏或动态栏。
     *
     * @param channel 栏目：1-公告，2-动态
     * @param limit   返回数量
     * @return 公告列表
     */
    List<AnnouncementMarqueeVO> selectByChannel(@Param("channel") int channel,
                                                @Param("limit") int limit);

    /**
     * 按 channel 查询首页公告，并按 resourceType 分组后对每组截取固定数量。
     *
     * @param channel      栏目，1-系统通知，2-业务动态
     * @param limitPerType 每个 resourceType 返回条数
     * @return 公告列表
     */
    List<AnnouncementMarqueeVO> selectByChannelGrouped(@Param("channel") int channel,
                                                       @Param("limitPerType") int limitPerType);

    /**
     * 按资源类型查询首页公告。
     *
     * @param resourceType 资源类型
     * @param limit        返回数量
     * @return 公告列表
     */
    List<AnnouncementMarqueeVO> selectByResourceType(@Param("resourceType") String resourceType,
                                                     @Param("limit") int limit);

    /**
     * 按资源类型和栏目查询首页公告。
     *
     * @param resourceType 资源类型
     * @param channel      栏目：1-系统通知，2-业务动态
     * @param limit        返回数量
     * @return 公告列表
     */
    List<AnnouncementMarqueeVO> selectByResourceTypeAndChannel(@Param("resourceType") String resourceType,
                                                               @Param("channel") int channel,
                                                               @Param("limit") int limit);
}

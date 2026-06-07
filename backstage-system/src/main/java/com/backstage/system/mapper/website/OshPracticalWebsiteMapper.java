package com.backstage.system.mapper.website;

import com.backstage.system.domain.dto.website.WebsiteQueryDTO;
import com.backstage.system.domain.vo.website.OshPracticalWebsiteVO;
import com.backstage.system.domain.website.OshPracticalWebsite;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
* @author 24333
* @description 针对表【osh_practical_website(实用网站表)】的数据库操作Mapper
* @createDate 2026-03-26 19:22:13
* @Entity generator.domain.OshPracticalWebsite
*/
public interface OshPracticalWebsiteMapper  {
    /**
     * 分页查询实用网站列表（支持按名称和标签筛选）
     * @param queryDTO 查询参数
     * @return 网站 VO 列表
     */
    List<OshPracticalWebsiteVO> selectWebsitePage(@Param("queryDTO") WebsiteQueryDTO queryDTO);

    /**
     * 增加网站点击次数
     * @param websiteId 网站 ID
     * @return 影响行数
     */
    int incrementClickCount(@Param("websiteId") Long websiteId);

    /**
     * 用户提交网站
     * @param website 提交的网站信息
     * @return 影响行数
     */
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")// 返回主键
    @Insert("insert into osh_practical_website (no, name, url, description, logo_url, status) values (#{no}, #{name}, #{url}, #{description}, #{logoUrl}, #{status})")
    int insertWebsite(OshPracticalWebsite website);

    /**
     * 根据 ID 查询网站信息
     * @param websiteId 网站 ID
     * @return 网站对象
     */
    @Select("SELECT * FROM osh_practical_website " +
            "WHERE id = #{websiteId} " +
            "AND delete_flag = 0 " +
            "AND status = 2")
    OshPracticalWebsite selectById(Long websiteId);

    /**
     * 更新网站审核信息
     * @param website 要更新的网站信息
     * @return 影响行数
     */
    @Update("UPDATE osh_practical_website " +
            "SET status = #{website.status}, " +
            "    audit_by = #{website.auditBy}, " +
            "    audit_time = #{website.auditTime}, " +
            "    reject_reason = #{website.rejectReason} " +
            "WHERE id = #{website.id} " +
            "  AND delete_flag = 0")
    Boolean updateStatusById(@Param("website") OshPracticalWebsite website);

    /**
     * 查询待审核的网站列表
     * @return 待审核的网站列表
     */
    @Select("SELECT * FROM osh_practical_website WHERE `delete_flag` = 0 AND `status` = 2")
    List<OshPracticalWebsite> selectAuditList();
/**
     * 根据网站ID批量删除网站
     * @param websiteIds 要删除的网站 ID 列表
     * @return 删除的网站数量
     */
    int batchDeleteWebsite(@Param("ids") List<Integer> websiteIds);

    @Select("SELECT * FROM osh_practical_website WHERE id = #{websiteId} AND delete_flag = 0 AND status = #{status}")
    OshPracticalWebsiteVO selectByIdAndStatus(@Param("websiteId") Long websiteId, @Param("status") Integer status);

    /**
     * 根据网站ID查询网站评分相关信息（用于更新网站评分信息时获取相关的评分数据）
     * @param websiteId 要查询的网站 ID
     * @return 网站对象
     */
    @Select("SELECT id, good_count, mid_count, bad_count, click_count, collection_count, create_time " +
            "FROM osh_practical_website " +
            "WHERE id = #{websiteId} AND delete_flag = 0 AND status = 4")
    OshPracticalWebsite selectByIdForUpdate(Long websiteId);

    @Update("UPDATE osh_practical_website SET rating_score = #{ratingScore} WHERE id = #{websiteId} AND delete_flag = 0 AND status = 4")
    int updateRatingScoreById(@Param("websiteId") Long websiteId, @Param("ratingScore") BigDecimal ratingScore);

    /**
     * 批量查询所有需要更新评分的网站(只查询评分计算需要的字段)
     * @return 网站列表
     */
    @Select("SELECT id, good_count, mid_count, bad_count, click_count, collection_count, create_time " +
            "FROM osh_practical_website " +
            "WHERE delete_flag = 0 AND status = 4")
    List<OshPracticalWebsite> selectAllWebsitesForRating();

    void addCount(@Param("websiteId") Long websiteId, @Param("ratingType") Integer ratingType);

    void updateCount(@Param("websiteId") Long websiteId, @Param("oldRatingType") Integer oldRatingType, @Param("ratingType") Integer ratingType);

    /**
     * 批量查询网站的评价计数（用于 ES 路径回填实时数据）
     */
    @Select("<script>" +
            "SELECT id, good_count, mid_count, bad_count " +
            "FROM osh_practical_website " +
            "WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND delete_flag = 0" +
            "</script>")
    List<OshPracticalWebsite> selectCountsByIds(@Param("ids") List<Long> ids);

    @Update("UPDATE osh_practical_website SET collection_count = collection_count + 1 WHERE id = #{websiteId} AND delete_flag = 0 AND status = 4")
    void addCollectionCount(Long websiteId);

    @Update("UPDATE osh_practical_website SET collection_count = collection_count - 1 WHERE id = #{websiteId} AND delete_flag = 0 AND status = 4")
    void reduceCollectionCount(Long websiteId);

    /**
     * 查询所有已审核通过的网站（含标签），用于全量同步到 ES
     * @return 已审核通过的网站 VO 列表
     */
    List<OshPracticalWebsiteVO> selectAllPublishedWebsites();
}





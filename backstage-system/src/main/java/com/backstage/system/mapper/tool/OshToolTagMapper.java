package com.backstage.system.mapper.tool;

import com.backstage.system.domain.tool.OshToolTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OshToolTagMapper {

    OshToolTag selectByName(@Param("name") String name);

    int insertToolTag(OshToolTag tag);

    int activateToolTag(@Param("id") Long id, @Param("operator") String operator);

    List<OshToolTag> selectAvailableTags(@Param("keyword") String keyword);

    List<OshToolTag> selectRecommendTags(@Param("limit") int limit);

    List<String> selectTagNamesByToolId(@Param("toolId") Long toolId);

    List<Long> selectTagIdsByToolId(@Param("toolId") Long toolId);

    int softDeleteRelationsByToolId(@Param("toolId") Long toolId, @Param("operator") String operator);

    int insertToolTagRel(@Param("toolId") Long toolId, @Param("tagId") Long tagId, @Param("operator") String operator);

    int increaseUseCount(@Param("tagId") Long tagId);
}

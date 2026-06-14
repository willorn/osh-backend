package com.backstage.system.mapper.resource;

import com.backstage.system.domain.resource.ResourceGroupResource;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 资源组-资源 关联数据层
 *
 * @author backstage
 */
@Mapper
public interface ResourceGroupResourceMapper extends BaseMapper<ResourceGroupResource> {

    @Select("SELECT DISTINCT " +
            " g.name" +
            " FROM backstage.osh_resource_group_resource r" +
            " LEFT JOIN osh_resource_group g ON r.group_id = g.id" +
            " WHERE r.delete_flag = 0 AND g.delete_flag = 0 AND resource_id = #{resId}")
    String selectResourceGroupId(@Param("resId") Long resId);
}

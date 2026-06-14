package com.backstage.system.service.resource.impl;

import com.backstage.common.enums.ResourceCodePrefixEnum;
import com.backstage.common.utils.generate.GenerateUtil;
import com.backstage.system.domain.resource.Resource;
import com.backstage.system.domain.resource.ResourceGroupResource;
import com.backstage.system.domain.vo.resource.ResourceVO;
import com.backstage.system.mapper.resource.ResourceGroupResourceMapper;
import com.backstage.system.mapper.resource.ResourceMapper;
import com.backstage.system.service.resource.IResourceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 内部资源 服务实现
 *
 * @author backstage
 */
@Service
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource> implements IResourceService {

    @javax.annotation.Resource
    private ResourceGroupResourceMapper resourceGroupResourceMapper;

    @Override
    public Page<Resource> pageResource(String keyword, Page<Resource> page) {
        LambdaQueryWrapper<Resource> wrapper = new LambdaQueryWrapper<Resource>()
                .like(StringUtils.isNotBlank(keyword), Resource::getName, keyword)
                .orderByDesc(Resource::getId);
        return this.page(page, wrapper);
    }

    @Override
    public Resource getResource(Long id) {
        return this.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createResource(Resource resource, Long groupId) {
        if (resource != null && (resource.getNo() == null || resource.getNo().isEmpty())) {
            resource.setNo(GenerateUtil.generateResourceCode(ResourceCodePrefixEnum.INTERNAL_RESOURCE));
        }
        this.save(resource);
        
        // 如果传入了groupId，创建资源与资源组的关联
        if (groupId != null) {
            ResourceGroupResource rel = new ResourceGroupResource();
            rel.setGroupId(groupId);
            rel.setResourceId(resource.getId());
            resourceGroupResourceMapper.insert(rel);
        }
        
        return resource.getId();
    }

    @Override
    public void updateResource(Resource resource) {
        this.updateById(resource);
    }

    @Override
    public void deleteResource(Long id) {
        this.removeById(id);
    }

    @Override
    public List<ResourceVO> listVOByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return baseMapper.selectVOByIds(ids);
    }
}

package com.backstage.system.service.resource.impl;

import cn.hutool.core.io.FileUtil;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.backstage.system.service.resource.impl.BaiduPanClient.generateFileName;
import static com.backstage.system.service.resource.impl.BaiduPanClient.getSystemTempDir;

/**
 * 内部资源 服务实现
 *
 * @author backstage
 */
@Service
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource> implements IResourceService {

    @javax.annotation.Resource
    private ResourceGroupResourceMapper resourceGroupResourceMapper;

    private final Logger log = LoggerFactory.getLogger(ResourceServiceImpl.class);

    @Value("${innerResource.baiduPan.appName:test}")
    private String appName;

    @Value("${innerResource.baiduPan.accessToken:}")
    private String accessToken;

    BaiduPanClient client;

    @PostConstruct
    public void init() {
        client = new BaiduPanClient(accessToken);
    }

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
        resource.setNo(UUID.randomUUID().toString());
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

    @Override
    public Resource upload(Long resId, MultipartFile file) {
        Resource resource = getById(resId);
        String resourceGroupName = resourceGroupResourceMapper.selectResourceGroupId(resId);
        // 所有文件路径必须在 /apps/{应用名}/ 下，应用名 = 你在百度开放平台注册时填的产品名称
        // 例如应用名叫 "osh"，则路径为 /apps/osh/xxx
        String remotePath = client.getPath(appName, "osh-resource", resourceGroupName, file.getOriginalFilename());
        try {
            client.upload(file, remotePath, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        resource.setFilePath(remotePath);
        resource.setFilePlatform("BaiduNetDisk");
        updateById(resource);
        return resource;
    }

    @Override
    public void download(Long resId, HttpServletResponse response) {
        Resource resource = getById(resId);
        String fileName = FileUtil.getName(resource.getFilePath());
        Path tmpFile = Paths.get(getSystemTempDir(), "osh_resource", fileName, generateFileName());
        Path path = null;
        try {
            path = client.download(resource.getFilePath(), tmpFile.toAbsolutePath().toString(), true);
            log.info("Download file from Baidu NetDisk, path: {}", path);
            try (InputStream inputStream = FileUtil.getInputStream(path.toFile())) {
                ResourceUtils.writeToResponse(fileName, inputStream, response);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (path != null) {
                FileUtil.del(path);
            }
        }
    }
}

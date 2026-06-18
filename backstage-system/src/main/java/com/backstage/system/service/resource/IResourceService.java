package com.backstage.system.service.resource;

import com.backstage.system.domain.resource.Resource;
import com.backstage.system.domain.vo.resource.ResourceVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 内部资源 服务接口
 *
 * @author backstage
 */
public interface IResourceService extends IService<Resource> {

    /**
     * 分页查询资源列表
     *
     * @param keyword 关键词
     * @param page    分页参数
     * @return 资源分页
     */
    Page<Resource> pageResource(String keyword, Page<Resource> page);

    /**
     * 查询资源详情
     *
     * @param id 资源ID
     * @return 资源
     */
    Resource getResource(Long id);

    /**
     * 新增资源
     *
     * @param resource 资源
     * @param groupId 资源组ID（可选）
     * @return 资源ID
     */
    Long createResource(Resource resource, Long groupId);

    /**
     * 修改资源
     *
     * @param resource 资源
     */
    void updateResource(Resource resource);

    /**
     * 删除资源（逻辑删除）
     *
     * @param id 资源ID
     */
    void deleteResource(Long id);

    /**
     * 按资源ID集合查询资源VO列表
     *
     * @param ids 资源ID集合
     * @return 资源VO集合
     */
    List<ResourceVO> listVOByIds(List<Long> ids);

    Resource upload(Long resId, MultipartFile file);

    void download(Long resId, HttpServletResponse response);
}

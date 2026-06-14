package com.backstage.system.controller.resource;

import com.backstage.common.annotation.OshUserEvent;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.vo.resource.ResourceVO;
import com.backstage.system.service.common.OssService;
import com.backstage.system.service.resource.IResourceService;
import com.backstage.system.service.resource.impl.ResourceUtils;
import com.backstage.system.utils.OssUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 内部资源 Controller
 *
 * @author backstage
 */
@ApiOperation(value = "内部资源接口")
@RestController
@RequestMapping("/pc/internal/resource")
public class ResourceController {

    @Resource
    private IResourceService resourceService;

    @Resource
    private OssService ossService;

    @Autowired
    private OssUtil ossUtil;

    @ApiOperation("资源分页列表")
    @OshUserEvent(module = "内部资源模块", actionType = "查询", description = "查询资源分页")
    // @PreAuthorize("hasAuthority('internal:resource:list')")
    @GetMapping("/page")
    public R<Page<com.backstage.system.domain.resource.Resource>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        Page<com.backstage.system.domain.resource.Resource> page = new Page<>(pageNum, pageSize);
        return R.ok(resourceService.pageResource(keyword, page));
    }

    @ApiOperation("全量资源列表（下拉选择）")
    // @PreAuthorize("hasAuthority('internal:resource:query')")
    @GetMapping("/list")
    public R<List<com.backstage.system.domain.resource.Resource>> list(@RequestParam(required = false) String keyword) {
        Page<com.backstage.system.domain.resource.Resource> page = new Page<>(1, 1000);
        return R.ok(resourceService.pageResource(keyword, page).getRecords());
    }

    @ApiOperation("资源详情")
    // @PreAuthorize("hasAuthority('internal:resource:query')")
    @GetMapping("/{id}")
    public R<com.backstage.system.domain.resource.Resource> detail(@PathVariable Long id) {
        return R.ok(resourceService.getResource(id));
    }

    @ApiOperation("新增资源")
    @OshUserEvent(module = "内部资源模块", actionType = "新增", description = "新增资源")
    // @PreAuthorize("hasAuthority('internal:resource:add')")
    @PostMapping
    public R<Long> create(@RequestBody java.util.Map<String, Object> params) {
        com.backstage.system.domain.resource.Resource resource = new com.backstage.system.domain.resource.Resource();
        resource.setName((String) params.get("name"));
        resource.setType((String) params.get("type"));
        resource.setRemark((String) params.get("remark"));

        // 获取groupId（如果存在）
        Long groupId = null;
        if (params.get("groupId") != null) {
            groupId = Long.valueOf(params.get("groupId").toString());
        }

        return R.ok(resourceService.createResource(resource, groupId));
    }

    @ApiOperation("修改资源")
    @OshUserEvent(module = "内部资源模块", actionType = "修改", description = "修改资源")
    // @PreAuthorize("hasAuthority('internal:resource:edit')")
    @PutMapping
    public R<Void> update(@RequestBody com.backstage.system.domain.resource.Resource resource) {
        resourceService.updateResource(resource);
        return R.ok();
    }

    @ApiOperation("删除资源")
    @OshUserEvent(module = "内部资源模块", actionType = "删除", description = "删除资源")
    // @PreAuthorize("hasAuthority('internal:resource:remove')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return R.ok();
    }

    @ApiOperation("按ID集合查询资源VO")
    // @PreAuthorize("hasAuthority('internal:resource:query')")
    @PostMapping("/vo-by-ids")
    public R<List<ResourceVO>> listVOByIds(@RequestBody List<Long> ids) {
        return R.ok(resourceService.listVOByIds(ids));
    }

    @ApiOperation("上传资源文件")
    @OshUserEvent(module = "内部资源模块", actionType = "上传", description = "上传资源文件")
    @PostMapping("/upload")
    public R<Map<String, Object>> uploadFile(@RequestParam String resourceId, @RequestParam("file") MultipartFile file) {
        try {
            resourceService.upload(Long.valueOf(resourceId), file);
            return R.ok();
        } catch (Exception e) {
            return R.fail("文件上传失败：" + e.getMessage());
        }
    }

    @ApiOperation("下载资源文件")
    // @OshUserEvent(module = "内部资源模块", actionType = "下载", description = "下载资源文件")
    @PostMapping("/download/{id}")
    public void downloadFile(@PathVariable Long id, HttpServletResponse response) {
        try {
            // 获取资源详情
            resourceService.download(id, response);
        } catch (Exception e) {
            ResourceUtils.writeError(response, e);
        }
    }
}

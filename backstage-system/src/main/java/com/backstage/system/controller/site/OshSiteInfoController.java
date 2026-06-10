package com.backstage.system.controller.site;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.Log;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.common.enums.BusinessType;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.course.OshCourse;
import com.backstage.system.domain.site.*;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.service.IOshCourseService;
import com.backstage.system.service.common.OssService;
import com.backstage.system.service.site.IOshSiteInfoService;
import com.backstage.system.service.site.IOshSiteTagsService;
import com.backstage.system.service.site.impl.DemoSiteConfig;
import com.backstage.system.service.user.IOshUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 内部网站信息 Controller
 *
 * @author backstage
 */
@Api(tags = "内部网站管理")
@RestController
@RequestMapping("/pc/site")
public class OshSiteInfoController extends BaseController {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    IOshSiteInfoService oshSiteInfoService;

    @Autowired
    IOshSiteTagsService oshSiteTagsService;

    @Autowired
    OssService ossService;

    @Autowired
    IOshUserService oshUserService;

    @Autowired
    IOshCourseService oshCourseService;

    /**
     * 查询网站列表
     */
    @Anonymous
    @ApiOperation("查询网站列表")
    @GetMapping("/list")
    public R<TableDataInfo> list(OshSiteInfo siteInfo) {
        List<OshSiteInfo> list = oshSiteInfoService.listSites(siteInfo);
        // 获取封面图片访问
        for (OshSiteInfo oshSiteInfo : list) {
            if (StringUtils.isNotEmpty(oshSiteInfo.getCover())) {
                // 30 分钟过期
                oshSiteInfo.setCover(ossService.getLimitedUrl(oshSiteInfo.getCover(), 30));
            }
        }
        Set<Long> siteIds = list.stream().map(OshSiteInfo::getId).collect(Collectors.toSet());
        Map<Long, List<OshSiteTag>> siteTagMap = oshSiteTagsService.getAllTag(siteIds)
                .stream()
                .collect(Collectors.groupingBy(OshSiteTag::getSiteId));
        for (OshSiteInfo oshSiteInfo : list) {
            oshSiteInfo.setTagList(siteTagMap.get(oshSiteInfo.getId()));
        }
        oshSiteInfoService.setRelatedResources(list);
        return R.ok(getDataTable(list));
    }

    @Anonymous
    @ApiOperation("获取网站详细信息")
    @GetMapping(value = "/{id}")
    public R<OshSiteInfo> getSiteInfo(@PathVariable Long id, @RequestParam(required = false, defaultValue = "false") Boolean needUrl) {
        OshSiteInfo siteInfo = oshSiteInfoService.getById(id);
        if (siteInfo == null) {
            return R.fail("网站不存在");
        }
        if (!needUrl) {
            siteInfo.setSiteUrl(null);
        }
        siteInfo.setTagList(oshSiteTagsService.getTagsBySiteId(id));
        return R.ok(siteInfo);
    }

    /**
     * 点击网站，统计网站使用
     */
    @Anonymous
    @ApiOperation("获取网站详细信息")
    @PostMapping(value = "/use/{id}")
    public R<OshSiteInfo> useSite(@PathVariable Long id) {
        OshSiteInfo siteInfo = oshSiteInfoService.getById(id);
        if (siteInfo == null) {
            return R.fail("网站不存在");
        }
        R<OshUser> userInfo = oshUserService.getUserInfo();
        oshSiteInfoService.insertUsage(siteInfo, userInfo.getData());
        return R.ok(siteInfo);
    }

    /**
     * 新增网站信息
     */
    @Anonymous
    @Log(title = "内部网站", businessType = BusinessType.INSERT)
    @ApiOperation("新增网站信息")
    @PostMapping
    public R<Void> add(@Validated @RequestBody OshSiteInfo siteInfo) {
        long currentUserId = ThreadLocalUtil.getCurrentUserId();
        siteInfo.setCreateBy(currentUserId);
        siteInfo.setCreateTime(LocalDateTime.now());
        siteInfo.setUpdateTime(LocalDateTime.now());
        siteInfo.setUpdateBy(currentUserId);
        siteInfo.setStatus(1);
        if (oshSiteInfoService.saveSiteInfo(siteInfo)) {
            // 保存标签
            if (siteInfo.getTagList() != null && !siteInfo.getTagList().isEmpty()) {
                oshSiteTagsService.saveSiteTags(siteInfo.getId(), siteInfo.getTagList(), currentUserId);
            }
            return R.ok();
        } else {
            return R.fail("新增失败");
        }
    }

    /**
     * 修改网站信息
     */
    @Anonymous
    @Log(title = "内部网站", businessType = BusinessType.UPDATE)
    @ApiOperation("修改网站信息")
    @PutMapping
    public R<Void> edit(@RequestBody OshSiteInfo siteInfo) {
        long currentUserId = ThreadLocalUtil.getCurrentUserId();
        siteInfo.setUpdateBy(currentUserId);
        siteInfo.setUpdateTime(LocalDateTime.now());
        if (oshSiteInfoService.updateSiteInfo(siteInfo)) {
            // 更新标签
            if (siteInfo.getTagList() != null) {
                oshSiteTagsService.saveSiteTags(siteInfo.getId(), siteInfo.getTagList(), currentUserId);
            }
            return R.ok();
        } else {
            return R.fail("修改失败");
        }
    }

    /**
     * 删除网站信息
     */
    @Anonymous
    @Log(title = "内部网站", businessType = BusinessType.DELETE)
    @ApiOperation("删除网站信息")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        if (oshSiteInfoService.removeByIds(Arrays.asList(ids))) {
            return R.ok();
        } else {
            return R.fail("删除失败");
        }
    }

    /**
     * 检查网站连接状态
     */
    @Anonymous
    @ApiOperation("检查网站连接状态")
    @PostMapping("/check")
    public R<Boolean> checkSiteConnection(@RequestBody List<String> ids) {
        List<OshSiteInfo> oshSiteInfos = oshSiteInfoService.listByIds(ids.stream().map(Long::valueOf).collect(Collectors.toList()));
        if (CollectionUtils.isEmpty(oshSiteInfos)) {
            return R.fail("网站不存在");
        }
        return R.ok(oshSiteInfoService.testConnection(oshSiteInfos));
    }

    /**
     * 检查网站连接状态
     */
    @Anonymous
    @ApiOperation("检查网站连接状态")
    @PostMapping("/check-all")
    public R<List<OshSiteInfo>> checkAllSiteConnection() {
        List<OshSiteInfo> oshSiteInfos = oshSiteInfoService.list();
        if (CollectionUtils.isEmpty(oshSiteInfos)) {
            return R.fail("网站不存在");
        }
        oshSiteInfoService.testConnection(oshSiteInfos);
        return R.ok(oshSiteInfos);
    }

    /**
     * 启动演示站点后端服务（同步阻塞）
     * 1. 通过 SSH 在远程服务器后台执行启动脚本
     * 2. 定期执行健康检查脚本，轮询直到服务启动成功或超时
     */
    @Anonymous
    @ApiOperation("启动演示站点后端服务（同步阻塞）")
    @PostMapping("/demo/start/{id}")
    public R<Map<String, Object>> startDemoService(@PathVariable Long id) {
        try {
            DemoSiteConfig cfg = oshSiteInfoService.loadDemoSiteConfig(id);
            oshSiteInfoService.requireConfigField("启动脚本", cfg.getStartupScript());
            oshSiteInfoService.requireConfigField("健康检查脚本", cfg.getHealthCheckScript());
            Map<String, Object> result = oshSiteInfoService.startDemo(cfg);
            return R.ok(result);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            log.error("启动演示服务失败", e);
            return R.fail("启动失败：" + e.getMessage());
        }
    }

    /**
     * 检查演示站点后端服务状态
     */
    @Anonymous
    @ApiOperation("检查演示站点后端服务状态")
    @PostMapping("/demo/check/{id}")
    public R<Map<String, Object>> checkDemoServiceStatus(@PathVariable Long id) {
        try {
            DemoSiteConfig cfg = oshSiteInfoService.loadDemoSiteConfig(id);
            oshSiteInfoService.requireConfigField("健康检查脚本", cfg.getHealthCheckScript());
            Map<String, Object> result = oshSiteInfoService.checkDemo(cfg);
            return R.ok(result);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            log.error("检查演示服务状态失败", e);
            return R.fail("检查失败：" + e.getMessage());
        }
    }

    /**
     * 异步启动演示站点（调用方轮询此接口）。
     * 首次调用提交后台启动任务；后续调用返回当前任务状态。
     */
    @ApiOperation("异步启动演示站点（轮询接口）")
    @PostMapping("/demo/start-async/{id}")
    public R<Map<String, Object>> startDemoAsync(@PathVariable Long id) {
        try {
            Map<String, Object> result = oshSiteInfoService.startDemoAsync(id);
            return R.ok(result);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            log.error("异步启动演示站点失败", e);
            return R.fail("启动失败：" + e.getMessage());
        }
    }

    /**
     * 停止演示站点服务
     */
    @Anonymous
    @ApiOperation("停止演示站点服务")
    @PostMapping("/demo/stop/{id}")
    public R<Map<String, Object>> stopDemoService(@PathVariable Long id) {
        try {
            DemoSiteConfig cfg = oshSiteInfoService.loadDemoSiteConfig(id);
            oshSiteInfoService.requireConfigField("停止脚本", cfg.getStopScript());
            Map<String, Object> result = oshSiteInfoService.stopDemo(cfg);
            return R.ok(result);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            log.error("停止演示服务失败", e);
            return R.fail("停止失败：" + e.getMessage());
        }
    }

    // ==================== 网站资源关联管理 ====================

    @Anonymous
    @ApiOperation("查询站点资源关联选项")
    @GetMapping("/resources/options")
    public R<List<SelectOption>> listResourceOptions() {
        List<SelectOption> options = new ArrayList<>();
        SelectOption course = new SelectOption();
        course.setLabel(SiteResourceType.COURSE.getDescription());
        course.setValue(SiteResourceType.COURSE.getType());
        List<OshCourse> oshCourses = oshCourseService.selectCourseList();
        course.setChildren(oshCourses.stream().map(oshCourse -> {
            SelectOption option = new SelectOption();
            option.setLabel(oshCourse.getTitle());
            option.setValue(String.valueOf(oshCourse.getId()));
            return option;
        }).collect(Collectors.toList()));
        options.add(course);

        return R.ok(options);
    }
}

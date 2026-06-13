package com.backstage.system.controller.website;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.OshResourceId;
import com.backstage.common.annotation.OshUserEvent;
import com.backstage.common.constant.ResourceType;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.announcement.vo.AnnouncementMarqueeVO;
import com.backstage.system.domain.dto.website.WebsiteAuditDTO;
import com.backstage.system.domain.dto.website.WebsiteQueryDTO;
import com.backstage.system.domain.dto.website.WebsiteRatingDTO;
import com.backstage.system.domain.dto.website.WebsiteSubmitDTO;
import com.backstage.system.domain.vo.website.OshPracticalWebsiteVO;
import com.backstage.system.domain.vo.website.WebsiteImportResultVO;
import com.backstage.system.service.website.IWebsiteAnnouncementService;
import com.backstage.system.service.website.OshPracticalWebsiteService;
import com.backstage.system.service.website.OshUserFavoriteWebsiteService;
import com.backstage.system.service.website.OshWebsiteTagService;
import com.backstage.system.service.website.OshWebsiteUserRatingService;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.backstage.system.utils.UserContextUtil.getCurrentUser;

/**
 * 实用网站 Controller
 */
@RestController
@RequestMapping("/pc/website")
public class OshPracticalWebsiteController extends BaseController {

    @Autowired
    private OshPracticalWebsiteService oshPracticalWebsiteService;

    @Autowired
    private OshUserFavoriteWebsiteService oshUserFavoriteWebsiteService;

    @Resource
    private OshWebsiteUserRatingService oshWebsiteUserRatingService;

    @Autowired
    private OshWebsiteTagService oshWebsiteTagService;

    @Autowired
    private IWebsiteAnnouncementService websiteAnnouncementService;

    /**
     * 查询实用网站公告栏（无需登录）
     */
    @Anonymous
    @ApiOperation("查询实用网站公告栏")
    @GetMapping("/notices")
    @OshUserEvent(module = "实用网站", actionType = "查询", resourceType = ResourceType.WEBSITE_TYPE, description = "查询实用网站公告", recordAnonymous = true)
    public R<List<AnnouncementMarqueeVO>> getNotices(
            @RequestParam(required = false, defaultValue = "10") int limit) {
        return R.ok(websiteAnnouncementService.getWebsiteNotices(limit));
    }

    /**
     * 查询实用网站动态栏（无需登录）
     */
    @Anonymous
    @ApiOperation("查询实用网站动态栏")
    @GetMapping("/dynamics")
    @OshUserEvent(module = "实用网站", actionType = "查询", resourceType = ResourceType.WEBSITE_TYPE, description = "查询实用网站动态", recordAnonymous = true)
    public R<List<AnnouncementMarqueeVO>> getDynamics(
            @RequestParam(required = false, defaultValue = "10") int limit) {
        return R.ok(websiteAnnouncementService.getWebsiteDynamics(limit));
    }

    /**
     * 查询标签列表（公开，支持关键字模糊搜索）
     * 参考课程模块 GET /pc/course/tags?keyword=
     */
    @Anonymous
    @ApiOperation("查询网站标签列表")
    @GetMapping("/tags")
    @OshUserEvent(module = "实用网站", actionType = "查询", resourceType = ResourceType.WEBSITE_TYPE, description = "查询网站标签", recordAnonymous = true)
    public R<List<Map<String, Object>>> getTags(
            @RequestParam(required = false) String keyword) {
        return R.ok(oshWebsiteTagService.searchTags(keyword));
    }

    /**
     * 查询实用网站列表（公开，游客可访问）
     */
    @Anonymous
    @ApiOperation("查询实用网站列表")
    @PostMapping("/list")
    @OshUserEvent(module = "实用网站", actionType = "查询", resourceType = ResourceType.WEBSITE_TYPE, description = "查询实用网站列表", recordAnonymous = true)
    public R<Map<String, Object>> list(@RequestBody WebsiteQueryDTO queryDTO) {
        List<OshPracticalWebsiteVO> list = oshPracticalWebsiteService.selectWebsitePage(queryDTO);
        PageInfo<OshPracticalWebsiteVO> oshPracticalWebsiteVoPageInfo = new PageInfo<>(list);
        Map<String, Object> data = new LinkedHashMap<>(4);
        data.put("rows", list);
        data.put("total", oshPracticalWebsiteVoPageInfo.getTotal());
        data.put("pageNum", oshPracticalWebsiteVoPageInfo.getPageNum());
        data.put("pageSize", oshPracticalWebsiteVoPageInfo.getPageSize());
        return R.ok(data, "ok");
    }

    /**
     * 增加网站点击次数（公开）
     */
    @Anonymous
    @ApiOperation("增加网站点击次数")
    @PutMapping("/click")
    @OshUserEvent(module = "实用网站", actionType = "点击", resourceType = ResourceType.WEBSITE_TYPE, description = "点击实用网站", recordAnonymous = true)
    public R<Void> incrementClickCount(@OshResourceId @RequestParam("id") Long id) {
        int result = oshPracticalWebsiteService.incrementClickCount(id);
        return result > 0 ? R.ok() : R.fail("网络开小差");
    }

    /**
     * 用户提交网站（需登录）
     */
    @ApiOperation("用户提交网站")
    @PostMapping("/submit")
    @OshUserEvent(module = "实用网站", actionType = "提交", resourceType = ResourceType.WEBSITE_TYPE, resourceNameExpression = "#p0.name", description = "提交网站")
    @PreAuthorize("hasAuthority('website:submit')")
    public R<String> submit(@RequestBody WebsiteSubmitDTO submitDto) {
        try {
            int result = oshPracticalWebsiteService.submitWebsite(submitDto);
            if (result > 0) {
                return R.ok();
            } else {
                return R.fail("提交失败，请稍后重试");
            }
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("网络开小差");
        }
    }

    /**
     * 用户收藏网站（需登录）
     */
    @ApiOperation("用户收藏网站")
    @PostMapping("/favorite")
    @OshUserEvent(module = "实用网站", actionType = "收藏", resourceType = ResourceType.WEBSITE_TYPE, description = "收藏网站")
    @PreAuthorize("hasAuthority('website:favorite')")
    public R<Void> favorite(@OshResourceId Long websiteId, @RequestParam(value = "remark", required = false) String remark) {
        try {
            int result = oshUserFavoriteWebsiteService.favoriteWebsite(websiteId);
            if (result > 0) {
                return R.ok();
            } else {
                return R.fail("收藏失败，请勿重复收藏");
            }
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("网络开小差");
        }
    }

    /**
     * 用户取消收藏网站（需登录）
     */
    @ApiOperation("用户取消收藏网站")
    @GetMapping("/del")
    @OshUserEvent(module = "实用网站", actionType = "取消收藏", resourceType = ResourceType.WEBSITE_TYPE, description = "取消收藏网站")
    @PreAuthorize("hasAuthority('website:favorite:cancel')")
    public R<Void> cancelFavorite(@OshResourceId @RequestParam("websiteId") Long websiteId) {
        try {
            int result = oshUserFavoriteWebsiteService.cancelFavoriteWebsite(websiteId);
            if (result > 0) {
                return R.ok();
            } else {
                return R.fail("取消收藏失败，请稍后重试");
            }
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("网络开小差");
        }
    }

    /**
     * 查询用户的收藏网站列表（需登录）
     */
    @ApiOperation("查询用户的收藏网站列表")
    @GetMapping("/Favorites")
    @OshUserEvent(module = "实用网站", actionType = "查询", resourceType = ResourceType.WEBSITE_TYPE, description = "查询用户收藏网站")
    @PreAuthorize("hasAuthority('website:favorite:list')")
    public R<Map<String, Object>> getMyFavoriteList(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            TableDataInfo result = oshUserFavoriteWebsiteService.selectUserFavoriteList(pageNum, pageSize);
            // 手动组装，避免 TableDataInfo 自带的 code/msg 字段污染响应结构
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("total", result.getTotal());
            data.put("rows", result.getRows());
            return R.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("网络开小差");
        }
    }

    /**
     * 管理员审核网站（需权限）
     */
    @ApiOperation("管理员审核网站")
    @PostMapping("/audit")
    @OshUserEvent(module = "实用网站", actionType = "审核", resourceType = ResourceType.WEBSITE_TYPE, description = "审核网站")
    @PreAuthorize("hasAuthority('website:audit')")
    public R<String> audit(@RequestBody WebsiteAuditDTO auditDto) {
        try {
            boolean auditResult = oshPracticalWebsiteService.auditWebsite(auditDto);
            if (auditResult) {
                // 通过和拒绝都算操作成功
                Integer status = auditDto.getStatus();
                String msg = (status == 4) ? "审核通过" : "已拒绝";
                return R.ok(msg);
            } else {
                return R.fail("审核操作失败，请稍后重试");
            }
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("审核失败");
        }
    }

    /**
     * 查询待审核的网站列表（需权限）
     */
    @ApiOperation("查询待审核网站列表")
    @GetMapping("/audit/list")
    @OshUserEvent(module = "实用网站", actionType = "查询", resourceType = ResourceType.WEBSITE_TYPE, description = "查询待审核网站")
    @PreAuthorize("hasAuthority('website:audit:list')")
    public R<TableDataInfo> getAuditByList(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            TableDataInfo result = oshPracticalWebsiteService.selectAuditList(pageNum, pageSize);
            return R.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("查询失败");
        }
    }

    /**
     * 根据 ID 查询待审核网站详情（需权限）
     */
    @ApiOperation("查询待审核网站详情")
    @GetMapping("/audit/detail/{websiteId}")
    @OshUserEvent(module = "实用网站", actionType = "浏览", resourceType = ResourceType.WEBSITE_TYPE, description = "浏览待审核网站详情")
    @PreAuthorize("hasAuthority('website:audit:detail')")
    public R<OshPracticalWebsiteVO> getAuditDetail(@OshResourceId @PathVariable Long websiteId) {
        try {
            if (websiteId == null) {
                return R.fail("网站ID不能为空");
            }
            OshPracticalWebsiteVO website = oshPracticalWebsiteService.getAuditDetail(websiteId);
            if (website == null) {
                return R.fail("网站不存在或已审核");
            }
            return R.ok(website);
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("查询失败");
        }
    }

    /**
     * 管理员批量删除网站（需权限）
     */
    @ApiOperation("批量删除网站")
    @GetMapping("/batch")
    @OshUserEvent(module = "实用网站", actionType = "删除", resourceType = ResourceType.WEBSITE_TYPE, description = "批量删除网站")
    @PreAuthorize("hasAuthority('website:delete:batch')")
    public R<String> batchDelete(@OshResourceId @RequestParam List<Integer> websiteIds) {
        try {
            int result = oshPracticalWebsiteService.batchDeleteWebsite(websiteIds);
            return R.ok("成功删除 " + result + " 个网站");
        } catch (ServiceException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("删除失败");
        }
    }

    /**
     * 用户提交网站评价（需登录）
     */
    @ApiOperation("提交网站评价")
    @PostMapping("/rating/submit")
    @OshUserEvent(module = "实用网站", actionType = "评价", resourceType = ResourceType.WEBSITE_TYPE, description = "提交网站评价")
    @PreAuthorize("hasAuthority('website:rating:submit')")
    public R<Void> submitRating(@RequestBody WebsiteRatingDTO ratingDTO) {
        try {
            Long userId = getCurrentUser().getId();
            if (userId == null) {
                return R.fail("请先登录");
            }
            oshWebsiteUserRatingService.submitRating(userId, ratingDTO.getWebsiteId(), ratingDTO.getRatingType());
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("评价失败，请稍后重试");
        }
    }

    // ===================== 批量导入 =====================

    /**
     * 下载导入模板（需登录，管理员和普通用户均可下载）
     */
    @ApiOperation("下载实用网站导入模板")
    @GetMapping("/import/template")
    @Anonymous
    public void downloadImportTemplate(HttpServletResponse response) {
        oshPracticalWebsiteService.downloadImportTemplate(response);
    }

    /**
     * 普通用户批量导入网站（导入后进入审核队列，status=2）
     */
    @ApiOperation("普通用户批量导入网站（待审核）")
    @PostMapping("/import")
    @OshUserEvent(module = "实用网站", actionType = "批量导入", description = "普通用户批量导入网站")
    @Anonymous // TODO: 测试完成后恢复 @PreAuthorize("hasAuthority('website:import')")
    public R<WebsiteImportResultVO> importWebsites(@RequestParam("file") MultipartFile file) {
        try {
            String operator = getCurrentUser().getUsername();
            WebsiteImportResultVO result = oshPracticalWebsiteService.batchImport(file, 2, operator);
            String msg = "导入完成：成功 " + result.getSuccessCount() + " 条，失败 " + result.getFailCount() + " 条";
            return R.ok(result, msg);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("导入失败，请稍后重试");
        }
    }

    /**
     * 管理员批量导入网站（直接发布，status=4）
     */
    @ApiOperation("管理员批量导入网站（直接发布）")
    @PostMapping("/import/admin")
    @OshUserEvent(module = "实用网站", actionType = "批量导入", description = "管理员批量导入网站并直接发布")
    @Anonymous // TODO: 测试完成后恢复 @PreAuthorize("hasAuthority('website:import:admin')")
    public R<WebsiteImportResultVO> adminImportWebsites(@RequestParam("file") MultipartFile file) {
        try {
            String operator = getCurrentUser().getUsername();
            WebsiteImportResultVO result = oshPracticalWebsiteService.batchImport(file, 4, operator);
            String msg = "导入完成：成功 " + result.getSuccessCount() + " 条，失败 " + result.getFailCount() + " 条";
            return R.ok(result, msg);
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("导入失败，请稍后重试");
        }
    }
}

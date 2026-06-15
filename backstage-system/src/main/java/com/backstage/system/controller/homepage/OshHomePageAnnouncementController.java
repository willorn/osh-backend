package com.backstage.system.controller.homepage;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.announcement.vo.AnnouncementMarqueeVO;
import com.backstage.system.service.homepage.IOshHomePageAnnouncementPushService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 首页公告栏 Controller
 */
@Api(tags = "首页-公告栏")
@RestController
@RequestMapping("/pc/homepage/announcement")
public class OshHomePageAnnouncementController extends BaseController {

    @Autowired
    private IOshHomePageAnnouncementPushService announcementPushService;

    @ApiOperation("查询首页系统通知(channel=1)，按 resourceType 分组，每组固定 5 条")
    @GetMapping("/notice")
    @Anonymous
    public R<Map<String, List<AnnouncementMarqueeVO>>> getSystemNotice() {
        return R.ok(announcementPushService.getSystemNotice());
    }

    @ApiOperation("查询首页业务动态(channel=2)，按 resourceType 分组，每组固定 5 条")
    @GetMapping("/dynamic")
    @Anonymous
    public R<Map<String, List<AnnouncementMarqueeVO>>> getBusinessDynamic() {
        return R.ok(announcementPushService.getBusinessDynamic());
    }
}

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

/**
 * 首页公告接口。
 */
@Api(tags = "首页-公告")
@RestController
@RequestMapping("/pc/homepage/announcement")
public class OshHomePageAnnouncementController extends BaseController {

    @Autowired
    private IOshHomePageAnnouncementPushService announcementPushService;

    @ApiOperation("查询首页系统通知，按 resourceType 分组，每组最近 5 条")
    @GetMapping("/notice")
    @Anonymous
    public R<List<AnnouncementMarqueeVO>> getSystemNotice() {
        return R.ok(announcementPushService.getSystemNotice());
    }

    @ApiOperation("查询首页业务动态，按 resourceType 分组，每组最近 5 条")
    @GetMapping("/dynamic")
    @Anonymous
    public R<List<AnnouncementMarqueeVO>> getBusinessDynamic() {
        return R.ok(announcementPushService.getBusinessDynamic());
    }
}

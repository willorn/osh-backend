package com.backstage.system.controller.course;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.course.vo.CourseAnnouncementVO;
import com.backstage.system.service.course.ICourseAnnouncementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Course module announcement endpoints (system notice + user dynamic feed)
@Api(tags = "课程公告")
@RestController
@RequestMapping("/pc/course/announcement")
public class CourseAnnouncementController {

    @Autowired
    private ICourseAnnouncementService courseAnnouncementService;

    @ApiOperation("查询课程模块系统通知")
    @GetMapping("/systemNotice/latest")
    @Anonymous
    public R<List<CourseAnnouncementVO>> listLatestSystemNotices() {
        return R.ok(courseAnnouncementService.listLatestSystemNotices());
    }

    @ApiOperation("查询课程模块业务公告")
    @GetMapping("/userNotice/latest")
    @Anonymous
    public R<List<CourseAnnouncementVO>> listLatestUserNotices() {
        return R.ok(courseAnnouncementService.listLatestUserNotices());
    }
}

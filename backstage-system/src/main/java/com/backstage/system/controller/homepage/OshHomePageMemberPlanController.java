package com.backstage.system.controller.homepage;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.member.OshMemberPlan;
import com.backstage.system.service.homepage.IOshHomePageMemberPlanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "首页-会员套餐")
@RestController
@RequestMapping("/pc/homepage/member")
public class OshHomePageMemberPlanController extends BaseController {

    @Resource
    private IOshHomePageMemberPlanService homePageMemberPlanService;

    @ApiOperation("查询首页会员套餐")
    @GetMapping("/plans")
    @Anonymous
    public R<List<OshMemberPlan>> plans() {
        return R.ok(homePageMemberPlanService.getPlans());
    }
}

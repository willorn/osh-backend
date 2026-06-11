package com.backstage.system.controller.behavior;

import com.backstage.common.annotation.OshUserLevel;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.behavior.BehaviorEventQuery;
import com.backstage.system.domain.behavior.ContributionCoefficientConfig;
import com.backstage.system.domain.behavior.ContributionQuery;
import com.backstage.system.domain.user.OshRole;
import com.backstage.system.enums.behavior.BehaviorActionType;
import com.backstage.system.enums.behavior.BehaviorModuleType;
import com.backstage.system.enums.behavior.ContributionResourceType;
import com.backstage.system.mapper.behavior.BehaviorStatsMapper;
import com.backstage.system.mapper.user.OshRoleMapper;
import com.backstage.system.service.behavior.ContributionCoefficientService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pc/admin/behavior")
public class BehaviorStatsController {

    @Resource
    private BehaviorStatsMapper behaviorStatsMapper;

    @Resource
    private ContributionCoefficientService contributionCoefficientService;

    @Resource
    private OshRoleMapper oshRoleMapper;

    @GetMapping("/meta")
    @OshUserLevel(6)
    public R meta() {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("roleLevels", listRoleLevelOptions());
        data.put("modules", BehaviorModuleType.options());
        data.put("resourceTypes", ContributionResourceType.options());
        data.put("actionTypes", BehaviorActionType.options());
        data.put("coefficients", contributionCoefficientService.getConfig());
        return R.ok(data);
    }

    @GetMapping("/contribution/coefficients")
    @OshUserLevel(6)
    public R getContributionCoefficients() {
        return R.ok(contributionCoefficientService.getConfig());
    }

    @PostMapping("/contribution/coefficients")
    @OshUserLevel(6)
    public R saveContributionCoefficients(@RequestBody(required = false) ContributionCoefficientConfig config) {
        return R.ok(contributionCoefficientService.saveConfig(config));
    }

    @PostMapping("/events/page")
    @OshUserLevel(6)
    public R pageEvents(@RequestBody BehaviorEventQuery query) {
        if (query == null) {
            query = new BehaviorEventQuery();
        }
        normalizePage(query);
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<Map<String, Object>> rows = behaviorStatsMapper.selectEventPage(query);
        PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(rows);
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("total", pageInfo.getTotal());
        data.put("rows", pageInfo.getList());
        return R.ok(data);
    }

    @PostMapping("/overview")
    @OshUserLevel(6)
    public R overview(@RequestBody(required = false) BehaviorEventQuery query) {
        if (query == null) {
            query = new BehaviorEventQuery();
        }
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("summary", behaviorStatsMapper.selectSummary(query));
        data.put("trend", behaviorStatsMapper.selectTrend(query, "day"));
        data.put("modules", behaviorStatsMapper.selectModuleDistribution(query));
        data.put("actions", behaviorStatsMapper.selectActionDistribution(query));
        data.put("activeUsers", behaviorStatsMapper.selectActiveUserRank(query));
        return R.ok(data);
    }

    @PostMapping("/trend")
    @OshUserLevel(6)
    public R trend(@RequestBody BehaviorEventQuery query,
                   @RequestParam(value = "grain", defaultValue = "day") String grain) {
        return R.ok(behaviorStatsMapper.selectTrend(query == null ? new BehaviorEventQuery() : query, grain));
    }

    @PostMapping("/contribution/summary")
    @OshUserLevel(6)
    public R contributionSummary(@RequestBody(required = false) ContributionQuery query) {
        return R.ok(behaviorStatsMapper.selectContributionSummary(prepareContributionQuery(query)));
    }

    @PostMapping("/contribution/resources")
    @OshUserLevel(6)
    public R contributionResources(@RequestBody(required = false) ContributionQuery query) {
        query = prepareContributionQuery(query);
        normalizePage(query);
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<Map<String, Object>> rows = behaviorStatsMapper.selectContributionResources(query);
        PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(rows);
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("total", pageInfo.getTotal());
        data.put("rows", pageInfo.getList());
        return R.ok(data);
    }

    @PostMapping("/contribution/revenue/page")
    @OshUserLevel(6)
    public R contributionRevenueDetails(@RequestBody(required = false) ContributionQuery query) {
        query = prepareContributionQuery(query);
        normalizePage(query);
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<Map<String, Object>> rows = behaviorStatsMapper.selectContributionRevenueDetails(query);
        PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(rows);
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("total", pageInfo.getTotal());
        data.put("rows", pageInfo.getList());
        return R.ok(data);
    }

    private void normalizePage(BehaviorEventQuery query) {
        if (query.getPageNum() == null || query.getPageNum() < 1) {
            query.setPageNum(1);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(20);
        }
        if (query.getPageSize() > 200) {
            query.setPageSize(200);
        }
    }

    private void normalizePage(ContributionQuery query) {
        if (query.getPageNum() == null || query.getPageNum() < 1) {
            query.setPageNum(1);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(20);
        }
        if (query.getPageSize() > 200) {
            query.setPageSize(200);
        }
    }

    private ContributionQuery prepareContributionQuery(ContributionQuery query) {
        if (query == null) {
            query = new ContributionQuery();
        }
        query.applyCoefficients(contributionCoefficientService.getConfig());
        return query;
    }

    private List<Map<String, Object>> listRoleLevelOptions() {
        List<OshRole> roles = oshRoleMapper.selectList(new LambdaQueryWrapper<OshRole>()
                .eq(OshRole::getDeleteFlag, 0)
                .orderByAsc(OshRole::getLevel)
                .orderByAsc(OshRole::getId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (OshRole role : roles) {
            if (role.getLevel() == null) {
                continue;
            }
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("label", "Lv." + role.getLevel() + " " + role.getRoleName());
            row.put("value", role.getLevel());
            row.put("roleCode", role.getRoleCode());
            rows.add(row);
        }
        return rows;
    }
}

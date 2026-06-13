package com.backstage.system.service.openproject.impl;

import com.backstage.system.domain.openproject.OshOpenProject;
import com.backstage.system.domain.openproject.OshOpenProjectStatsSnapshot;
import com.backstage.system.domain.openproject.vo.OpenProjectRankVO;
import com.backstage.system.mapper.openproject.OshOpenProjectMapper;
import com.backstage.system.mapper.openproject.OshOpenProjectStatsSnapshotMapper;
import com.backstage.system.service.openproject.IOshOpenProjectRankService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OshOpenProjectRankServiceImpl implements IOshOpenProjectRankService {

    private static final Logger log = LoggerFactory.getLogger(OshOpenProjectRankServiceImpl.class);
    private static final int DEFAULT_PERIOD = 7;
    private static final int DEFAULT_TOP_N = 10;
    private static final int MAX_TOP_N = 50;

    @Autowired
    private OshOpenProjectMapper projectMapper;

    @Autowired
    private OshOpenProjectStatsSnapshotMapper snapshotMapper;

    @Override
    public List<OpenProjectRankVO> getRank(String rankType, int period, int topN) {
        rankType = normalizeRankType(rankType);
        period = normalizePeriod(period);
        topN = normalizeTopN(topN);
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(period);

        // 查今天的快照（最新数据）
        List<OshOpenProjectStatsSnapshot> todaySnapshots = snapshotMapper.selectByDate(today);
        if (todaySnapshots.isEmpty()) {
            // 今天还没有快照，用昨天的
            todaySnapshots = snapshotMapper.selectByDate(today.minusDays(1));
        }
        if (todaySnapshots.isEmpty()) {
            todaySnapshots = buildCurrentSnapshots();
        }
        if (todaySnapshots.isEmpty()) return Collections.emptyList();

        // 查 period 天前的快照
        List<OshOpenProjectStatsSnapshot> startSnapshots = snapshotMapper.selectByDate(startDate);
        Map<Long, OshOpenProjectStatsSnapshot> startMap = startSnapshots.stream()
                .collect(Collectors.toMap(OshOpenProjectStatsSnapshot::getProjectId, s -> s));

        // 计算增量
        List<long[]> increments = new ArrayList<>(); // [projectId, increment]
        for (OshOpenProjectStatsSnapshot today_ : todaySnapshots) {
            OshOpenProjectStatsSnapshot start = startMap.get(today_.getProjectId());
            int todayVal = "star".equals(rankType) ? today_.getStarCount() : today_.getForkCount();
            int startVal = start != null
                    ? ("star".equals(rankType) ? start.getStarCount() : start.getForkCount())
                    : 0;
            int increment = todayVal - startVal;
            increments.add(new long[]{today_.getProjectId(), increment, todayVal});
        }

        // 按增量降序排序，取 topN
        increments.sort((a, b) -> Long.compare(b[1], a[1]));
        List<long[]> top = increments.stream().limit(topN).collect(Collectors.toList());

        if (top.isEmpty()) return Collections.emptyList();

        // 批量查项目信息
        List<Long> projectIds = top.stream().map(a -> a[0]).collect(Collectors.toList());
        List<OshOpenProject> projects = projectMapper.selectList(
                new LambdaQueryWrapper<OshOpenProject>()
                        .in(OshOpenProject::getId, projectIds)
                        .eq(OshOpenProject::getStatus, 1)
                        .eq(OshOpenProject::getDeleteFlag, (byte) 0)
        );
        Map<Long, OshOpenProject> projectMap = projects.stream()
                .collect(Collectors.toMap(OshOpenProject::getId, p -> p));

        // 组装 VO
        List<OpenProjectRankVO> result = new ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            long[] item = top.get(i);
            Long projectId = item[0];
            OshOpenProject project = projectMap.get(projectId);
            if (project == null) continue;

            OpenProjectRankVO vo = new OpenProjectRankVO();
            vo.setRank(i + 1);
            vo.setId(project.getId());
            vo.setProjectName(project.getProjectName());
            vo.setProjectDesc(project.getProjectDesc());
            vo.setProjectUrl(project.getProjectUrl());
            vo.setProjectCover(project.getProjectCover());
            vo.setStarCount(project.getStarCount());
            vo.setForkCount(project.getForkCount());
            if ("star".equals(rankType)) {
                vo.setStarIncrement((int) item[1]);
            } else {
                vo.setForkIncrement((int) item[1]);
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public void saveTodaySnapshot() {
        LocalDate today = LocalDate.now();

        List<OshOpenProject> projects = projectMapper.selectList(
                new LambdaQueryWrapper<OshOpenProject>()
                        .eq(OshOpenProject::getStatus, 1)
                        .eq(OshOpenProject::getDeleteFlag, (byte) 0)
        );

        int inserted = 0, updated = 0;
        for (OshOpenProject project : projects) {
            OshOpenProjectStatsSnapshot existing =
                    snapshotMapper.selectByProjectAndDate(project.getId(), today);

            int starCount = project.getStarCount() != null ? project.getStarCount() : 0;
            int forkCount = project.getForkCount() != null ? project.getForkCount() : 0;

            if (existing == null) {
                // 今天还没有快照，插入
                OshOpenProjectStatsSnapshot snapshot = new OshOpenProjectStatsSnapshot();
                snapshot.setProjectId(project.getId());
                snapshot.setStarCount(starCount);
                snapshot.setForkCount(forkCount);
                snapshot.setSnapshotDate(today);
                snapshot.setDeleted(false);
                snapshotMapper.insert(snapshot);
                inserted++;
            } else {
                // 今天已有快照（手动多次调用），更新为最新值
                snapshotMapper.update(null,
                        new LambdaUpdateWrapper<OshOpenProjectStatsSnapshot>()
                                .eq(OshOpenProjectStatsSnapshot::getId, existing.getId())
                                .set(OshOpenProjectStatsSnapshot::getStarCount, starCount)
                                .set(OshOpenProjectStatsSnapshot::getForkCount, forkCount)
                );
                updated++;
            }
        }
        log.info("今日快照保存完成，新增={}，更新={}", inserted, updated);
    }

    private List<OshOpenProjectStatsSnapshot> buildCurrentSnapshots() {
        List<OshOpenProject> projects = projectMapper.selectList(
                new LambdaQueryWrapper<OshOpenProject>()
                        .eq(OshOpenProject::getStatus, 1)
                        .eq(OshOpenProject::getDeleteFlag, (byte) 0)
        );
        List<OshOpenProjectStatsSnapshot> snapshots = new ArrayList<>();
        for (OshOpenProject project : projects) {
            OshOpenProjectStatsSnapshot snapshot = new OshOpenProjectStatsSnapshot();
            snapshot.setProjectId(project.getId());
            snapshot.setStarCount(project.getStarCount() == null ? 0 : project.getStarCount());
            snapshot.setForkCount(project.getForkCount() == null ? 0 : project.getForkCount());
            snapshot.setSnapshotDate(LocalDate.now());
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    private String normalizeRankType(String rankType) {
        return "fork".equalsIgnoreCase(rankType) ? "fork" : "star";
    }

    private int normalizePeriod(int period) {
        return period == 30 ? 30 : DEFAULT_PERIOD;
    }

    private int normalizeTopN(int topN) {
        if (topN <= 0) {
            return DEFAULT_TOP_N;
        }
        return Math.min(topN, MAX_TOP_N);
    }
}

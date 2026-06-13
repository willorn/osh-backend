package com.backstage.quartz.task;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.backstage.system.domain.openproject.OshOpenProject;
import com.backstage.system.mapper.openproject.OshOpenProjectMapper;
import com.backstage.system.service.openproject.IOshOpenProjectRankService;
import com.backstage.system.service.openproject.IOshOpenProjectSourceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 开源项目 GitHub 数据同步定时任务
 * 调用方式（在 sys_job 表中配置）：
 *   bean 名称：openProjectSyncTask
 *   方法名称：syncAll
 *   cron：0 0 2 * * ?  （每天凌晨 2 点执行）
 */
@Component("openProjectSyncTask")
public class OpenProjectSyncTask {

    private static final Logger log = LoggerFactory.getLogger(OpenProjectSyncTask.class);

    // GitHub API 速率限制：未认证 60次/小时，建议配置 token
    private static final String GITHUB_API = "https://api.github.com/repos/";
    // 从 GitHub URL 提取 owner/repo 的正则
    private static final Pattern GITHUB_PATTERN = Pattern.compile("github\\.com/([^/]+)/([^/\\s?#]+)");

    @Value("${github.token:}")
    private String githubToken;

    @Autowired
    private OshOpenProjectMapper projectMapper;

    @Autowired(required = false)
    private IOshOpenProjectRankService rankService;

    @Autowired(required = false)
    private IOshOpenProjectSourceService sourceService;

    /**
     * 全量同步所有已通过审核的开源项目的 GitHub 数据
     * xxl-job handler 名称：osh-backend-githubsync
     */
    @XxlJob("osh-backend-githubsync")
    public void syncAll() {
        XxlJobHelper.log("开源项目 GitHub 数据同步开始");
        log.info("开源项目 GitHub 数据同步开始");

        if (sourceService != null) {
            int synced = sourceService.syncAllEnabledSources();
            log.info("开源项目源同步完成，同步仓库数={}", synced);
            XxlJobHelper.log("开源项目源同步完成，同步仓库数=%d", synced);
            if (rankService != null) {
                try {
                    rankService.saveTodaySnapshot();
                } catch (Exception e) {
                    log.error("保存今日快照失败：{}", e.getMessage());
                }
            }
            return;
        }

        // 只同步已通过审核的项目
        List<OshOpenProject> projects =
                projectMapper.selectList(
                        new LambdaQueryWrapper<OshOpenProject>()
                                .eq(OshOpenProject::getStatus, 1)
                                .eq(OshOpenProject::getDeleteFlag, (byte) 0)
                );

        int success = 0, skip = 0, fail = 0;
        for (OshOpenProject project : projects) {
            String[] ownerRepo = extractOwnerRepo(project.getProjectUrl());
            if (ownerRepo == null) {
                skip++;
                continue;
            }
            try {
                syncOne(project.getId(), ownerRepo[0], ownerRepo[1]);
                success++;
                // 避免触发 GitHub API 速率限制，每次请求间隔 1 秒
                Thread.sleep(200);
            } catch (Exception e) {
                fail++;
                log.warn("同步失败，projectId={}, url={}, 原因={}", project.getId(), project.getProjectUrl(), e.getMessage());
            }
        }

        log.info("开源项目 GitHub 数据同步完成，成功={}，跳过={}，失败={}", success, skip, fail);
        XxlJobHelper.log("开源项目 GitHub 数据同步完成，成功=%d，跳过=%d，失败=%d", success, skip, fail);

        // 同步完成后保存今日快照，用于排行榜增量计算
        if (rankService != null) {
            try {
                rankService.saveTodaySnapshot();
            } catch (Exception e) {
                log.error("保存今日快照失败：{}", e.getMessage());
            }
        }
    }

    /**
     * 同步单个项目（可手动触发）
     */
    public void syncOne(Long projectId, String owner, String repo) throws Exception {
        String apiUrl = GITHUB_API + owner + "/" + repo;
        String json = httpGet(apiUrl);
        JSONObject data = JSON.parseObject(json);

        int starCount   = data.getIntValue("stargazers_count");
        int forkCount   = data.getIntValue("forks_count");
        boolean archived = data.getBooleanValue("archived");

        // 最近提交时间从 pushed_at 字段获取
        String pushedAt = data.getString("pushed_at");
        LocalDateTime lastCommitTime = null;
        if (pushedAt != null) {
            // GitHub 返回格式：2024-01-15T10:30:00Z
            lastCommitTime = LocalDateTime.parse(
                    pushedAt.replace("Z", ""),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            );
        }

        LocalDateTime now = LocalDateTime.now();
        final LocalDateTime finalLastCommitTime = lastCommitTime;

        projectMapper.update(null,
                new LambdaUpdateWrapper<OshOpenProject>()
                        .eq(OshOpenProject::getId, projectId)
                        .set(OshOpenProject::getStarCount, starCount)
                        .set(OshOpenProject::getForkCount, forkCount)
                        .set(OshOpenProject::getLastCommitTime, finalLastCommitTime)
                        .set(OshOpenProject::getIsArchived, (byte) (archived ? 1 : 0))
                        .set(OshOpenProject::getLastSyncTime, now)
        );

        log.debug("同步成功，projectId={}, star={}, fork={}, archived={}", projectId, starCount, forkCount, archived);
    }

    /**
     * 从项目 URL 中提取 GitHub owner/repo
     * 支持：<a href="https://github.com/owner/repo">...</a> 或 <a href="https://github.com/owner/repo.git">...</a>
     */
    private String[] extractOwnerRepo(String url) {
        if (url == null) return null;
        Matcher m = GITHUB_PATTERN.matcher(url);
        if (!m.find()) return null;
        String owner = m.group(1);
        String repo  = m.group(2).replaceAll("\\.git$", "");
        return new String[]{owner, repo};
    }

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        conn.setRequestProperty("User-Agent", "osh-backend");
        if (StringUtils.hasText(githubToken)) {
            conn.setRequestProperty("Authorization", "token " + githubToken);
        }
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);

        int code = conn.getResponseCode();
        if (code != 200) throw new RuntimeException("GitHub API 返回 " + code);

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}

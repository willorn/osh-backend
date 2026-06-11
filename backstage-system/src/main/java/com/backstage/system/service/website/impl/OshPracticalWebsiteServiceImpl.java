package com.backstage.system.service.website.impl;

import com.backstage.common.annotation.DistributeLock;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.common.enums.ResourceCodePrefixEnum;
import com.backstage.common.utils.StringUtils;
import com.backstage.common.utils.email.EmailUtil;
import com.backstage.common.utils.generate.GenerateUtil;
import com.backstage.common.utils.redis.DistributedLockUtil;
import com.backstage.system.domain.dto.website.WebsiteAuditDTO;
import com.backstage.system.domain.dto.website.WebsiteQueryDTO;
import com.backstage.system.domain.dto.website.WebsiteSubmitDTO;
import com.backstage.system.domain.vo.website.EsPageResult;
import com.backstage.system.domain.vo.website.OshPracticalWebsiteVO;
import com.backstage.system.domain.website.OshPracticalWebsite;
import com.backstage.system.domain.website.WebsiteEsDoc;
import com.backstage.system.enums.behavior.ContributionResourceType;
import com.backstage.system.mapper.website.OshPracticalWebsiteMapper;
import com.backstage.system.mapper.website.OshWebsiteTagRelMapper;
import com.backstage.system.mapper.website.OshWebsiteUserRatingMapper;
import com.backstage.system.service.behavior.ContributionService;
import com.backstage.system.service.website.OshPracticalWebsiteService;
import com.backstage.system.service.website.OshWebsiteTagService;
import com.backstage.system.utils.UserContextUtil;
import com.backstage.system.utils.WebsiteRatingCalculatorUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
//import com.sun.org.apache.bcel.internal.generic.NEW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 24333
 * @description 针对表【osh_practical_website(实用网站表)】的数据库操作Service实现
 * @createDate 2026-03-26 19:22:13
 */
@Service
public class OshPracticalWebsiteServiceImpl implements OshPracticalWebsiteService {
    private static final Logger log = LoggerFactory.getLogger(OshPracticalWebsiteServiceImpl.class);
    @Autowired
    private OshPracticalWebsiteMapper oshPracticalWebsiteMapper;
    @Autowired
    private OshWebsiteTagRelMapper oshWebsiteTagRelMapper;
    @Autowired
    private OshWebsiteTagService oshWebsiteTagService;
    @Autowired
    private OshWebsiteUserRatingMapper oshWebsiteUserRatingMapper;
    @Autowired
    private EmailUtil emailUtil;
    @Autowired
    private DistributedLockUtil distributedLockUtil;
    @Autowired
    private WebsiteEsService websiteEsService;
    @Autowired
    private ContributionService contributionService;
    /**
     * 查询网站列表
     *
     * @param queryDTO
     * @return
     */
    @Override
    public List<OshPracticalWebsiteVO> selectWebsitePage(WebsiteQueryDTO queryDTO) {
        if (queryDTO == null) {
            queryDTO = new WebsiteQueryDTO();
        }
        // 获取当前用户 ID（游客为 null，不查评价状态）
        Long currentUserId = UserContextUtil.getCurrentUserId();

        // 第一步：先查 ES
        EsPageResult<OshPracticalWebsiteVO> esResult = websiteEsService.searchFromEs(queryDTO);
        if (esResult != null && esResult.getTotal() != 0) {
            int pageNum = queryDTO.getPageNum() == null ? 1 : queryDTO.getPageNum();
            int pageSize = queryDTO.getPageSize() == null ? 10 : queryDTO.getPageSize();

            List<OshPracticalWebsiteVO> voList = esResult.getList();
            if (!voList.isEmpty()) {
                List<Long> ids = voList.stream().map(OshPracticalWebsiteVO::getId).collect(Collectors.toList());
                // ES 是快照，回填 MySQL 最新计数，保证实时性
                fillCountsFromDb(voList, ids);
                fillMyRatingType(voList, ids, currentUserId);
            }

            Page<OshPracticalWebsiteVO> page = new Page<>(pageNum, pageSize);
            page.addAll(voList);
            page.setTotal(esResult.getTotal());
            log.info("ES 搜索命中，共 {} 条", esResult.getTotal());
            return page;
        }

        // 第二步：ES 查不到或不可用，走 MySQL
        log.info("ES 未命中或不可用，降级走 MySQL 查询");
        Integer pageNum = queryDTO.getPageNum();
        Integer pageSize = queryDTO.getPageSize();
        PageHelper.startPage(pageNum, pageSize);
        List<OshPracticalWebsiteVO> list = oshPracticalWebsiteMapper.selectWebsitePage(queryDTO);
        if (!list.isEmpty()) {
            List<Long> ids = list.stream().map(OshPracticalWebsiteVO::getId).collect(Collectors.toList());
            fillMyRatingType(list, ids, currentUserId);
        }
        return list;
    }

    /**
     * 从 MySQL 主表批量回填 goodCount/midCount/badCount（用于 ES 路径保证实时性）
     */
    private void fillCountsFromDb(List<OshPracticalWebsiteVO> voList, List<Long> ids) {
        try {
            List<OshPracticalWebsite> dbList = oshPracticalWebsiteMapper.selectCountsByIds(ids);
            Map<Long, OshPracticalWebsite> dbMap = dbList.stream()
                    .collect(Collectors.toMap(OshPracticalWebsite::getId, w -> w));
            for (OshPracticalWebsiteVO vo : voList) {
                OshPracticalWebsite db = dbMap.get(vo.getId());
                if (db != null) {
                    vo.setGoodCount(db.getGoodCount());
                    vo.setMidCount(db.getMidCount());
                    vo.setBadCount(db.getBadCount());
                }
            }
        } catch (Exception e) {
            log.warn("回填评价计数失败，使用 ES 快照数据", e);
        }
    }

    /**
     * 批量回填当前用户的评价类型（myRatingType）
     * 游客（userId=null）跳过，所有 VO 的 myRatingType 保持 null
     */
    private void fillMyRatingType(List<OshPracticalWebsiteVO> voList, List<Long> ids, Long userId) {
        if (userId == null || ids.isEmpty()) {
            return;
        }
        try {
            List<Map<String, Object>> ratingList =
                    oshWebsiteUserRatingMapper.selectRatingTypesByUserAndWebsites(userId, ids);
            Map<Long, Integer> ratingMap = new HashMap<>();
            for (Map<String, Object> row : ratingList) {
                Long websiteId = ((Number) row.get("websiteId")).longValue();
                Integer ratingType = ((Number) row.get("ratingType")).intValue();
                ratingMap.put(websiteId, ratingType);
            }
            for (OshPracticalWebsiteVO vo : voList) {
                vo.setMyRatingType(ratingMap.get(vo.getId()));
            }
        } catch (Exception e) {
            log.warn("回填用户评价状态失败", e);
        }
    }
    /**
     * 递增点击次数
     *
     * @param websiteId
     * @return
     */
    @Override
    public int incrementClickCount(Long websiteId) {
        if (websiteId == null) {
            return 0;
        }
        return oshPracticalWebsiteMapper.incrementClickCount(websiteId);
    }
    /**
     * 提交网站
     *
     * @param submitDto
     * @return
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributeLock(scene = "website_submit", key = "website_submit_lock", includeUserId = true, waitTime = 0)
    public int submitWebsite(WebsiteSubmitDTO submitDto) {
        // 1. 参数校验（必填字段检查）
        if (submitDto == null ||
                submitDto.getName() == null || submitDto.getName().trim().isEmpty() ||
                submitDto.getUrl() == null || submitDto.getUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("网站名称和链接不能为空");
        }
        // 2. URL 格式简单校验
        String url = submitDto.getUrl().trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("网站链接格式不正确，请以 http:// 或 https:// 开头");
        }

        // 3. 构建网站对象
        OshPracticalWebsite website = new OshPracticalWebsite();
        website.setNo(GenerateUtil.generateResourceCode(ResourceCodePrefixEnum.WEBSITE));
        website.setName(submitDto.getName().trim());
        website.setUrl(url);
        website.setDescription(submitDto.getDescription());
        website.setLogoUrl(submitDto.getLogoUrl());
        website.setStatus(0);  // 0=待审核状态
        website.setClickCount(0); // 初始点击次数为 0
        website.setDeleteFlag(0);
        website.setCreateBy(UserContextUtil.getCurrentUser().getUsername());
        //website.setCreateBy("admin");

        // 4. 插入网站主表
        int websiteResult = oshPracticalWebsiteMapper.insertWebsite(website);
        if (websiteResult <= 0) {
            throw new RuntimeException("网站数据保存失败");
        }
        contributionService.recordContribution(ContributionResourceType.WEBSITE.getCode(), website.getId(), website.getName());

        // 5. 处理标签关联（tagNames 可选，为空则跳过）
        // 参考课程模块：标签不存在时自动创建，并维护 use_count
        if (submitDto.getTagNames() != null && !submitDto.getTagNames().isEmpty()) {
            oshWebsiteTagService.bindWebsiteTags(
                    website.getId(),
                    submitDto.getTagNames(),
                    website.getCreateBy()
            );
        }

        // 6. 发送邮件通知（失败不影响主流程）
        try {
            emailUtil.sendNewWebsiteSubmitEmail(
                    website.getId(),
                    website.getName(),
                    website.getUrl(),
                    website.getDescription(),
                    website.getCreateBy(),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
            );
        } catch (Exception e) {
            log.error("网站提交邮件发送失败，websiteId={}", website.getId(), e);
        }

        // 7. 返回网站插入结果
        return websiteResult;
    }
    /**
     * 审核网站
     *
     * @param auditDto
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean auditWebsite(WebsiteAuditDTO auditDto) {
        if (auditDto == null || auditDto.getWebsiteId() == null) {
            throw new IllegalArgumentException("网站 ID 不能为空");
        }
        if (auditDto.getStatus() == null ||
                (auditDto.getStatus() != 1 && auditDto.getStatus() != 2)) {
            throw new IllegalArgumentException("审核状态必须是 1（通过）或 2（拒绝）");
        }
        if (auditDto.getStatus() == 2 &&
                StringUtils.isEmpty(auditDto.getRejectReason())) {
            throw new IllegalArgumentException("拒绝时必须填写拒绝原因");
        }
        // 2. 查询网站是否存在
        OshPracticalWebsite website = oshPracticalWebsiteMapper.selectById(auditDto.getWebsiteId());
        if (website == null) {
            throw new IllegalArgumentException("网站不存在");
        }

        // 更新审核信息
        website.setStatus(auditDto.getStatus());
        website.setAuditBy("admin");
        website.setAuditTime(new Date());
        if (auditDto.getStatus() == 2) {
            // 如果拒绝，记录拒绝原因
            website.setRejectReason(auditDto.getRejectReason());
            boolean rejectResult = oshPracticalWebsiteMapper.updateStatusById(website);
            return rejectResult;
        }

        // 更新对应数据库
        boolean updateResult = oshPracticalWebsiteMapper.updateStatusById(website);
        if (updateResult) {
            // MySQL 更新成功后，把数据同步到 ES
            try {
                OshPracticalWebsiteVO vo = oshPracticalWebsiteMapper.selectByIdAndStatus(
                        auditDto.getWebsiteId(), 1);
                if (vo != null) {
                    WebsiteEsDoc doc = convertVoToEsDoc(vo);
                    websiteEsService.saveToEs(doc);
                }
            } catch (Exception e) {
                log.error("审核通过后同步 ES 失败，websiteId={}", auditDto.getWebsiteId(), e);
            }
        }
        return updateResult;
    }
    /**
     * 把 VO 转换成 ES 文档对象
     */
    private WebsiteEsDoc convertVoToEsDoc(OshPracticalWebsiteVO vo) {
        WebsiteEsDoc doc = new WebsiteEsDoc();
        doc.setId(vo.getId());
        doc.setName(vo.getName());
        doc.setUrl(vo.getUrl());
        doc.setDescription(vo.getDescription());
        doc.setLogoUrl(vo.getLogoUrl());
        doc.setClickCount(vo.getClickCount());
        doc.setGoodCount(vo.getGoodCount());
        doc.setMidCount(vo.getMidCount());
        doc.setBadCount(vo.getBadCount());
        doc.setCollectionCount(vo.getCollectionCount());
        doc.setRatingScore(vo.getRatingScore());
        doc.setAuditTime(vo.getAuditTime());
        // tags 从逗号分隔字符串转成 List
        if (vo.getTags() != null && !vo.getTags().isEmpty()) {
            doc.setTags(Arrays.asList(vo.getTags().split(",")));
        }
        return doc;
    }
    /**
     * 查询待审核列表
     *
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public TableDataInfo selectAuditList(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        // 查询待审核的网站（status = 0）
        List<OshPracticalWebsite> list = oshPracticalWebsiteMapper.selectAuditList();
        PageInfo<OshPracticalWebsite> pageInfo = new PageInfo<>(list);
        return new TableDataInfo(pageInfo.getList(), pageInfo.getTotal());
    }
    /**
     * 批量删除网站
     *
     * @param websiteIds
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int batchDeleteWebsite(List<Integer> websiteIds) {
        // 1. 批量软删除网站主表
        oshPracticalWebsiteMapper.batchDeleteWebsite(websiteIds);
        // 2. 批量软删除网站标签关联表
        return oshWebsiteTagRelMapper.deleteByWebsiteIds(websiteIds);
    }

    @Override
    public OshPracticalWebsiteVO getAuditDetail(Long websiteId) {
        if (websiteId == null) {
            return null;
        }
        return oshPracticalWebsiteMapper.selectByIdAndStatus(websiteId, 0);
    }
    /**
     * 更新网站评分
     *
     * @param websiteId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWebsiteRatingScore(Long websiteId) {
        if (websiteId == null) {
            return;
        }
        try {
            OshPracticalWebsite websiteEvaluation = oshPracticalWebsiteMapper.selectByIdForUpdate(websiteId);
            if (websiteEvaluation != null) {
                BigDecimal ratingScore = WebsiteRatingCalculatorUtil.calculateRatingScore(
                        websiteEvaluation.getGoodCount(),
                        websiteEvaluation.getMidCount(),
                        websiteEvaluation.getBadCount(),
                        websiteEvaluation.getClickCount(),
                        websiteEvaluation.getCollectionCount(),
                        websiteEvaluation.getCreateTime()
                );
                oshPracticalWebsiteMapper.updateRatingScoreById(websiteId, ratingScore);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 批量更新所有网站评分
     */
    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    public void batchUpdateAllWebsiteRatingScores() {
        try {
            List<OshPracticalWebsite> websitesEvaluation = oshPracticalWebsiteMapper.selectAllWebsitesForRating();
            if (websitesEvaluation == null || websitesEvaluation.isEmpty()) {
                log.info("没有需要更新评分的网站");
                return;
            }
            log.info("开始批量更新评分,共 {} 个网站", websitesEvaluation.size());
            int successCount = 0;
            int failCount = 0;
            for (OshPracticalWebsite website : websitesEvaluation) {
                try {
                    BigDecimal ratingScore = WebsiteRatingCalculatorUtil.calculateRatingScore(
                            website.getGoodCount(),
                            website.getMidCount(),
                            website.getBadCount(),
                            website.getClickCount(),
                            website.getCollectionCount(),
                            website.getCreateTime()
                    );
                    int result = oshPracticalWebsiteMapper.updateRatingScoreById(website.getId(), ratingScore);
                    if (result > 0) {
                        successCount++;
                    } else {
                        failCount++;
                        log.error("更新网站ID={}的评分失败", website.getId());

                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("更新网站ID={}的评分异常", website.getId(), e);
                    e.printStackTrace();
                }
            }
            log.info("批量更新评分完成，共更新 {} 条记录", successCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}





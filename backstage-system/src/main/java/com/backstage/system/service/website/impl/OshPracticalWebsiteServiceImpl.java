package com.backstage.system.service.website.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.backstage.common.annotation.DistributeLock;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.common.enums.ResourceCodePrefixEnum;
import com.backstage.common.utils.StringUtils;
import com.backstage.common.utils.email.EmailUtil;
import com.backstage.common.utils.generate.GenerateUtil;
import com.backstage.common.utils.redis.DistributedLockUtil;
import com.backstage.system.domain.dto.website.WebsiteAuditDTO;
import com.backstage.system.domain.dto.website.WebsiteImportDTO;
import com.backstage.system.domain.dto.website.WebsiteQueryDTO;
import com.backstage.system.domain.dto.website.WebsiteSubmitDTO;
import com.backstage.system.domain.vo.website.EsPageResult;
import com.backstage.system.domain.vo.website.OshPracticalWebsiteVO;
import com.backstage.system.domain.vo.website.WebsiteImportResultVO;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 实用网站 Service 实现
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
        website.setStatus(2);  // 2=待审核，与统一审核模块状态对齐
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
                (auditDto.getStatus() != 4 && auditDto.getStatus() != 6)) {
            throw new IllegalArgumentException("审核状态必须是 4（通过）或 6（拒绝）");
        }
        if (auditDto.getStatus() == 6 &&
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
        if (auditDto.getStatus() == 6) {
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
        // 查询待审核的网站（status = 2）
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

    // ===================== 批量导入 =====================

    /**
     * 批量导入网站（Excel）
     * 管理员：status=4 直接发布；普通用户：status=2 待审核
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WebsiteImportResultVO batchImport(MultipartFile file, int status, String operator) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要导入的 Excel 文件");
        }
        // 单次限制 500 条，防止超时
        final int MAX_ROWS = 500;

        List<WebsiteImportDTO> dataList = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), WebsiteImportDTO.class,
                    new AnalysisEventListener<WebsiteImportDTO>() {
                        @Override
                        public void invoke(WebsiteImportDTO data, AnalysisContext context) {
                            dataList.add(data);
                        }
                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                        }
                    }).sheet().headRowNumber(1).doRead();
        } catch (IOException e) {
            log.error("读取 Excel 文件失败", e);
            throw new RuntimeException("Excel 文件解析失败，请确认文件格式正确");
        }

        if (dataList.isEmpty()) {
            return new WebsiteImportResultVO(0, 0, Collections.emptyList());
        }
        if (dataList.size() > MAX_ROWS) {
            throw new IllegalArgumentException("单次最多导入 " + MAX_ROWS + " 条，当前文件包含 " + dataList.size() + " 条");
        }

        int successCount = 0;
        List<WebsiteImportResultVO.FailDetail> failDetails = new ArrayList<>();
        // Excel 内部去重：记录本次已处理过的 URL，防止同一文件内重复行
        Set<String> processedUrls = new HashSet<>();

        for (int i = 0; i < dataList.size(); i++) {
            // 行号从 2 开始（第 1 行是表头）
            int rowNum = i + 2;
            WebsiteImportDTO dto = dataList.get(i);

            // 1. 基础校验
            String validationError = validateImportRow(dto);
            if (validationError != null) {
                failDetails.add(new WebsiteImportResultVO.FailDetail(rowNum, dto.getName(), validationError));
                continue;
            }

            String url = dto.getUrl().trim();

            // 2. Excel 内部重复检查
            if (processedUrls.contains(url)) {
                failDetails.add(new WebsiteImportResultVO.FailDetail(rowNum, dto.getName(), "与文件内第" + (processedUrls.size()) + "行重复，已跳过"));
                continue;
            }

            // 3. 数据库重复检查
            if (oshPracticalWebsiteMapper.countByUrl(url) > 0) {
                failDetails.add(new WebsiteImportResultVO.FailDetail(rowNum, dto.getName(), "该网站链接已存在，已跳过"));
                continue;
            }

            processedUrls.add(url);

            try {
                // 构建实体
                OshPracticalWebsite website = new OshPracticalWebsite();
                website.setNo(GenerateUtil.generateResourceCode(ResourceCodePrefixEnum.WEBSITE));
                website.setName(dto.getName().trim());
                website.setUrl(url);
                website.setDescription(dto.getDescription());
                website.setLogoUrl(dto.getLogoUrl());
                website.setStatus(status);
                website.setClickCount(0);
                website.setGoodCount(0);
                website.setMidCount(0);
                website.setBadCount(0);
                website.setCollectionCount(0);
                website.setDeleteFlag(0);
                website.setCreateBy(operator);
                website.setCreateTime(new Date());
                // 管理员直接发布时记录审核信息
                if (status == 4) {
                    website.setAuditBy(operator);
                    website.setAuditTime(new Date());
                }

                // 插入主表
                int insertResult = oshPracticalWebsiteMapper.insertWebsite(website);
                if (insertResult <= 0) {
                    failDetails.add(new WebsiteImportResultVO.FailDetail(rowNum, dto.getName(), "数据库写入失败"));
                    continue;
                }

                // 处理标签（可选）
                if (dto.getTags() != null && !dto.getTags().trim().isEmpty()) {
                    String[] tagArr = dto.getTags().split(",");
                    List<String> tagNames = new ArrayList<>();
                    for (String tag : tagArr) {
                        String trimmed = tag.trim();
                        if (!trimmed.isEmpty()) {
                            tagNames.add(trimmed);
                        }
                    }
                    if (!tagNames.isEmpty()) {
                        oshWebsiteTagService.bindWebsiteTags(website.getId(), tagNames, operator);
                    }
                }

                // 管理员导入直接发布时，同步到 ES
                if (status == 4) {
                    try {
                        OshPracticalWebsiteVO vo = oshPracticalWebsiteMapper.selectByIdAndStatus(website.getId(), 4);
                        if (vo != null) {
                            websiteEsService.saveToEs(convertVoToEsDoc(vo));
                        }
                    } catch (Exception esEx) {
                        log.warn("第 {} 行同步 ES 失败，不影响导入结果，websiteId={}", rowNum, website.getId(), esEx);
                    }
                }

                successCount++;
            } catch (Exception e) {
                log.error("第 {} 行导入异常，name={}", rowNum, dto.getName(), e);
                failDetails.add(new WebsiteImportResultVO.FailDetail(rowNum, dto.getName(), "系统异常：" + e.getMessage()));
            }
        }

        return new WebsiteImportResultVO(successCount, failDetails.size(), failDetails);
    }

    /**
     * 校验导入行，返回错误信息；通过则返回 null
     */
    private String validateImportRow(WebsiteImportDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return "网站名称不能为空";
        }
        if (dto.getUrl() == null || dto.getUrl().trim().isEmpty()) {
            return "网站链接不能为空";
        }
        String url = dto.getUrl().trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "网站链接格式不正确，请以 http:// 或 https:// 开头";
        }
        if (dto.getName().trim().length() > 100) {
            return "网站名称不能超过 100 个字符";
        }
        if (url.length() > 500) {
            return "网站链接不能超过 500 个字符";
        }
        return null;
    }

    /**
     * 生成导入模板并写入响应流
     */
    @Override
    public void downloadImportTemplate(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("实用网站导入模板", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            // 写入带示例行的模板
            List<WebsiteImportDTO> exampleList = new ArrayList<>();
            WebsiteImportDTO example = new WebsiteImportDTO();
            example.setName("示例网站");
            example.setUrl("https://example.com");
            example.setDescription("这是一个示例网站描述");
            example.setLogoUrl("https://example.com/logo.png");
            example.setTags("工具,效率");
            exampleList.add(example);

            EasyExcel.write(response.getOutputStream(), WebsiteImportDTO.class)
                    .sheet("实用网站")
                    .doWrite(exampleList);
        } catch (IOException e) {
            log.error("生成导入模板失败", e);
            throw new RuntimeException("模板生成失败");
        }
    }


}





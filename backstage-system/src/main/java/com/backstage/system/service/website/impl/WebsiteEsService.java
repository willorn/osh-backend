package com.backstage.system.service.website.impl;

import com.backstage.common.response.PageResponse;
import com.backstage.system.domain.dto.website.WebsiteQueryDTO;
import com.backstage.system.domain.vo.website.EsPageResult;
import com.backstage.system.domain.vo.website.OshPracticalWebsiteVO;
import com.backstage.system.domain.website.OshPracticalWebsite;
import com.backstage.system.domain.website.WebsiteEsDoc;
import com.backstage.system.mapper.website.OshPracticalWebsiteMapper;
import com.backstage.system.mapper.website.OshWebsiteEsMapper;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 实用网站 ES Service
 *
 * 职责：业务逻辑层，不直接操作 ES，委托给 OshWebsiteEsMapper
 *   1. 搜索网站（含降级逻辑：ES 异常时返回 null，上层降级走 MySQL）
 *   2. 单条写入 ES（审核通过时调用）
 *   3. 全量同步（先清空再批量写入）
 *   4. 重建索引
 *   5. 评价提交后同步计数到 ES（方案一：低频写保证实时性）
 */
@Service
public class WebsiteEsService {

    private static final Logger log = LoggerFactory.getLogger(WebsiteEsService.class);

    private static final String WEBSITE_INDEX = "osh_practical_website";

    @Autowired
    private OshWebsiteEsMapper oshWebsiteEsMapper;

    @Autowired
    private OshPracticalWebsiteMapper oshPracticalWebsiteMapper;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * 从 ES 搜索网站列表
     *
     * @param queryDTO 查询条件
     * @return 搜索结果；ES 不可用时返回 null，上层降级走 MySQL
     */
    public EsPageResult<OshPracticalWebsiteVO> searchFromEs(WebsiteQueryDTO queryDTO) {
        try {
            PageResponse<OshPracticalWebsiteVO> pageResponse = oshWebsiteEsMapper.searchWebsites(queryDTO);
            log.info("ES 查询结果: total={}, rows={}", pageResponse.getTotal(), pageResponse.getRows().size());
            return new EsPageResult<>(pageResponse.getRows(), pageResponse.getTotal());
        } catch (Exception e) {
            log.error("ES 搜索网站失败，将降级走 MySQL 查询", e);
            return null;
        }
    }

    /**
     * 向 ES 写入单条网站文档
     * 在网站审核通过时调用，把 MySQL 数据同步到 ES
     *
     * @param doc 要写入的网站文档
     */
    public void saveToEs(WebsiteEsDoc doc) {
        try {
            oshWebsiteEsMapper.bulkUpsertWebsites(Collections.singletonList(doc));
            log.info("网站 ID={} 同步到 ES 成功", doc.getId());
        } catch (Exception e) {
            log.error("网站 ID={} 同步到 ES 失败", doc.getId(), e);
        }
    }

    /**
     * 评价提交后，把 MySQL 最新的评价计数同步到 ES。
     * 只更新 goodCount/midCount/badCount 三个字段，不覆盖其他字段。
     * 失败不影响主流程，只打警告日志。
     *
     * @param websiteId 网站 ID
     */
    public void syncCountsToEs(Long websiteId) {
        try {
            OshPracticalWebsite website = oshPracticalWebsiteMapper.selectByIdForUpdate(websiteId);
            if (website == null) {
                return;
            }
            int goodCount = website.getGoodCount() == null ? 0 : website.getGoodCount();
            int midCount  = website.getMidCount()  == null ? 0 : website.getMidCount();
            int badCount  = website.getBadCount()  == null ? 0 : website.getBadCount();

            // 用 ES Update API 局部更新，不影响其他字段
            Request request = new Request("POST", "/" + WEBSITE_INDEX + "/_update/" + websiteId);
            request.setJsonEntity(String.format(
                    "{\"doc\":{\"goodCount\":%d,\"midCount\":%d,\"badCount\":%d}}",
                    goodCount, midCount, badCount));
            restHighLevelClient.getLowLevelClient().performRequest(request);

            log.info("网站 ID={} 评价计数同步到 ES 成功 good={} mid={} bad={}",
                    websiteId, goodCount, midCount, badCount);
        } catch (Exception e) {
            log.warn("网站 ID={} 评价计数同步到 ES 失败，不影响主流程", websiteId, e);
        }
    }

    /**
     * 全量同步网站到 ES
     * 从 MySQL 查出所有已审核通过的网站，先清空 ES 再批量写入
     *
     * @return 成功同步的文档数量
     */
    public int syncAllToEs() {
        List<OshPracticalWebsiteVO> voList = oshPracticalWebsiteMapper.selectAllPublishedWebsites();
        if (voList == null || voList.isEmpty()) {
            log.info("没有已审核通过的网站，跳过全量同步");
            return 0;
        }

        List<WebsiteEsDoc> docs = new ArrayList<>(voList.size());
        for (OshPracticalWebsiteVO vo : voList) {
            docs.add(convertVoToEsDoc(vo));
        }

        try {
            int deleted = oshWebsiteEsMapper.deleteAllWebsites();
            log.info("全量同步前清空 ES，共删除 {} 条文档", deleted);
            int synced = oshWebsiteEsMapper.bulkUpsertWebsites(docs);
            log.info("全量同步完成，共写入 {} 条文档", synced);
            return synced;
        } catch (Exception e) {
            throw new IllegalStateException("全量同步网站到 ES 失败", e);
        }
    }

    /**
     * 重建 ES 索引
     *
     * @param indexDefinitionJson 索引定义 JSON（来自 osh_practical_website_index.json）
     */
    public void recreateIndex(String indexDefinitionJson) {
        try {
            oshWebsiteEsMapper.recreateWebsiteSearchIndex(indexDefinitionJson);
            log.info("网站 ES 索引重建成功");
        } catch (Exception e) {
            throw new IllegalStateException("重建网站 ES 索引失败", e);
        }
    }

    /**
     * VO 转 ES 文档
     * tags 字段：VO 里是逗号分隔字符串，EsDoc 里是 List<String>
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
        if (vo.getTags() != null && !vo.getTags().isEmpty()) {
            doc.setTags(
                Arrays.stream(vo.getTags().split(","))
                      .map(String::trim)
                      .filter(s -> !s.isEmpty())
                      .collect(Collectors.toList())
            );
        }
        return doc;
    }
}

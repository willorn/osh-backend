package com.backstage.system.mapper.seckill;

import com.backstage.common.response.PageResponse;
import com.backstage.system.config.properties.SearchEsProperties;
import com.backstage.system.domain.seckill.es.SeckillItemEsDocument;
import com.backstage.system.domain.vo.seckill.SeckillActivityItemVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.ElasticsearchStatusException;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 秒杀商品明细 ES DAO
 * 对应索引 osh_seckill_item_search
 */
@Component
public class SeckillItemEsMapper {

    private static final String SECKILL_ITEM_SEARCH_INDEX = "osh_seckill_item_search";

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SearchEsProperties searchEsProperties;

    /**
     * 搜索秒杀商品明细
     * 固定过滤：startTime <= now、endTime > now、deleteFlag=0
     *
     * @param keyword     商品名称关键词（可为 null）
     * @param goodsType   商品类型过滤（可为 null）
     * @param tagNameList 标签名称列表过滤（可为 null）
     * @param pageNum     页码（从 1 开始）
     * @param pageSize    每页大小
     */
    public PageResponse<SeckillActivityItemVO> searchItems(
            String keyword, Integer goodsType, List<String> tagNameList, int pageNum, int pageSize) throws Exception {

        if (!restHighLevelClient.indices().exists(
                new GetIndexRequest(SECKILL_ITEM_SEARCH_INDEX), RequestOptions.DEFAULT)) {
            return PageResponse.of(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        SearchRequest searchRequest = new SearchRequest(SECKILL_ITEM_SEARCH_INDEX);
        searchRequest.source(buildSearchSource(keyword, goodsType, tagNameList, pageNum, pageSize));

        SearchResponse searchResponse;
        try {
            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException ex) {
            if (ex.status() != null && ex.status().getStatus() == 404) {
                return PageResponse.of(new ArrayList<>(), 0L, pageNum, pageSize);
            }
            throw ex;
        }

        if (searchResponse.isTimedOut()) {
            throw new IllegalStateException("search seckill items from es timed out");
        }

        List<SeckillActivityItemVO> rows = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            SeckillItemEsDocument doc = objectMapper.convertValue(hit.getSourceAsMap(), SeckillItemEsDocument.class);
            rows.add(toVO(doc));
        }

        return PageResponse.of(rows, searchResponse.getHits().getTotalHits().value, pageNum, pageSize);
    }

    /**
     * 批量 upsert 秒杀明细文档（全量同步时使用）
     */
    public int bulkUpsertItems(List<SeckillItemEsDocument> documents) throws Exception {
        if (documents == null || documents.isEmpty()) {
            return 0;
        }
        BulkRequest bulkRequest = new BulkRequest();
        for (SeckillItemEsDocument doc : documents) {
            bulkRequest.add(new IndexRequest(SECKILL_ITEM_SEARCH_INDEX)
                    .id(String.valueOf(doc.getId()))
                    .source(objectMapper.writeValueAsString(doc), XContentType.JSON));
        }
        BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (bulkResponse.hasFailures()) {
            throw new IllegalStateException(
                    "bulk upsert seckill items to es failed: " + bulkResponse.buildFailureMessage());
        }
        return documents.size();
    }

    /**
     * 清空索引内所有文档（全量同步前调用）
     */
    public int deleteAllItems() throws Exception {
        GetIndexRequest getIndexRequest = new GetIndexRequest(SECKILL_ITEM_SEARCH_INDEX);
        if (!restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT)) {
            return 0;
        }
        Request request = new Request("POST", "/" + SECKILL_ITEM_SEARCH_INDEX + "/_delete_by_query");
        request.addParameter("refresh", "true");
        request.addParameter("conflicts", "proceed");
        request.setJsonEntity("{\"query\":{\"match_all\":{}}}");
        Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
        String json = EntityUtils.toString(response.getEntity(), "UTF-8");
        return objectMapper.readTree(json).path("deleted").asInt(0);
    }

    /**
     * 重建索引（删旧建新，用于 mapping 变更时）
     */
    public void recreateSeckillItemSearchIndex(String indexDefinitionJson) throws Exception {
        GetIndexRequest getIndexRequest = new GetIndexRequest(SECKILL_ITEM_SEARCH_INDEX);
        if (restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT)) {
            restHighLevelClient.indices().delete(
                    new DeleteIndexRequest(SECKILL_ITEM_SEARCH_INDEX), RequestOptions.DEFAULT);
        }
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(SECKILL_ITEM_SEARCH_INDEX);
        createIndexRequest.source(indexDefinitionJson, XContentType.JSON);
        restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
    }

    // ==================== 私有方法 ====================

    private SearchSourceBuilder buildSearchSource(
            String keyword, Integer goodsType, List<String> tagNameList, int pageNum, int pageSize) {

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .from((pageNum - 1) * pageSize)
                .size(pageSize)
                .timeout(TimeValue.timeValueMillis(searchEsProperties.getFallbackTimeoutMillis()));

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                // 方式A：查询时过滤未开始和已结束活动，只返回当前时间窗口内的活动
                .filter(QueryBuilders.rangeQuery("startTime").lte("now"))
                .filter(QueryBuilders.rangeQuery("endTime").gt("now"))
                .filter(QueryBuilders.termQuery("deleteFlag", 0));

        // 关键词搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            boolQuery.must(QueryBuilders.matchQuery("title", keyword.trim()));
            sourceBuilder.sort(SortBuilders.scoreSort().order(SortOrder.DESC));
        }

        // 商品类型过滤
        if (goodsType != null) {
            boolQuery.filter(QueryBuilders.termQuery("goodsType", goodsType));
        }

        // 标签过滤
        if (tagNameList != null && !tagNameList.isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("tagNames", tagNameList));
        }

        sourceBuilder.query(boolQuery);
        // 默认按 sort 字段升序（运营配置的展示顺序），再按 seckillPrice 升序
        sourceBuilder.sort(SortBuilders.fieldSort("sort").order(SortOrder.ASC));
        sourceBuilder.sort(SortBuilders.fieldSort("seckillPrice").order(SortOrder.ASC));
        return sourceBuilder;
    }

    private SeckillActivityItemVO toVO(SeckillItemEsDocument doc) {
        SeckillActivityItemVO vo = new SeckillActivityItemVO();
        vo.setId(doc.getId());
        vo.setActivityId(doc.getActivityId());
        vo.setActivityStatus(doc.getActivityStatus());
        vo.setActivityTitle(doc.getActivityTitle());
        vo.setPayTimeoutMin(doc.getPayTimeoutMin());
        vo.setGoodsId(doc.getGoodsId());
        vo.setGoodsType(doc.getGoodsType());
        vo.setNo(doc.getNo());
        vo.setTitle(doc.getTitle());
        vo.setCover(doc.getCover());
        vo.setOriginPrice(doc.getOriginPrice());
        vo.setSeckillPrice(doc.getSeckillPrice());
        vo.setTotalStock(doc.getTotalStock());
        vo.setAvailableStock(doc.getAvailableStock());
        vo.setSoldCount(doc.getSoldCount());
        vo.setLimitPerUser(doc.getLimitPerUser());
        vo.setSort(doc.getSort());
        vo.setTagNames(doc.getTagNames());
        if (doc.getStartTime() != null) {
            vo.setStartTime(java.util.Date.from(
                    doc.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        }
        if (doc.getEndTime() != null) {
            vo.setEndTime(java.util.Date.from(
                    doc.getEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        }
        return vo;
    }
}

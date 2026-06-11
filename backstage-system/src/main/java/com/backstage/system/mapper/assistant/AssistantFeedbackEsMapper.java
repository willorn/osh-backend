package com.backstage.system.mapper.assistant;

import cn.hutool.core.util.StrUtil;
import com.backstage.common.response.PageResponse;
import com.backstage.system.config.properties.SearchEsProperties;
import com.backstage.system.domain.assistant.AssistantFeedback;
import com.backstage.system.domain.assistant.AssistantTicketStatus;
import com.backstage.system.domain.assistant.dto.AssistantFeedbackPageDTO;
import com.backstage.system.domain.assistant.es.AssistantFeedbackEsDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 反馈 ES 查询 Mapper
 *
 * <p>索引命名规范：
 * <ul>
 *   <li>物理索引：{@code osh_assistant_feedback_search_v1}（带版本号，mapping 变更时递增）</li>
 *   <li>读别名：{@code osh_assistant_feedback_search}（所有读操作面向此别名，不写死物理索引名）</li>
 * </ul>
 * mapping 定义在 {@code resources/es/osh_assistant_feedback_search_v1.json}，
 * 版本升级时新建文件（如 v2.json），不改旧文件，配合 {@code rebuildIndex} 做零停机切换。
 * </p>
 *
 * @author backstage
 */
@Component
public class AssistantFeedbackEsMapper {

    /**
     * 读别名：所有查询/写入均面向此别名，不直接使用物理索引名
     */
    private static final String FEEDBACK_READ_ALIAS = "osh_assistant_feedback_search";
    /**
     * 当前物理索引名（与 mapping 文件版本号对齐）
     */
    public static final String FEEDBACK_INDEX_V1 = "osh_assistant_feedback_search_v1";
    /**
     * 重建时新物理索引名前缀
     */
    private static final String FEEDBACK_INDEX_PREFIX = FEEDBACK_INDEX_V1 + "_";
    /**
     * mapping 资源文件路径
     */
    private static final String MAPPING_RESOURCE = "es/osh_assistant_feedback_search_v1.json";

    private static final String QUERY_MODE_MINE = "mine";
    private static final String DEFAULT_SORT_TYPE = "hot";
    private static final String SORT_TYPE_RELATED = "related";
    private static final DateTimeFormatter INDEX_SUFFIX_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final RestHighLevelClient restHighLevelClient;
    private final ObjectMapper objectMapper;
    private final SearchEsProperties searchEsProperties;

    public AssistantFeedbackEsMapper(RestHighLevelClient restHighLevelClient,
                                     ObjectMapper objectMapper,
                                     SearchEsProperties searchEsProperties) {
        this.restHighLevelClient = restHighLevelClient;
        this.objectMapper = objectMapper;
        this.searchEsProperties = searchEsProperties;
    }

    public PageResponse<AssistantFeedback> searchFeedbacks(AssistantFeedbackPageDTO dto, List<Long> favoriteFeedbackIds) throws Exception {
        int pageNum = resolvePageNum(dto);
        int pageSize = resolvePageSize(dto);
        if (favoriteFeedbackIds != null && favoriteFeedbackIds.isEmpty()) {
            return PageResponse.of(Collections.emptyList(), 0L, pageNum, pageSize);
        }
        ensureAliasExists();

        // 查询面向读别名，mapping 升级切换别名后此处无需改动
        SearchRequest searchRequest = new SearchRequest(FEEDBACK_READ_ALIAS);
        searchRequest.source(buildSearchSource(dto, pageNum, pageSize, favoriteFeedbackIds));
        SearchResponse searchResponse;
        try {
            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException exception) {
            if (exception.status() != null && exception.status().getStatus() == 404) {
                throw new IllegalStateException("feedback search index missing", exception);
            }
            throw exception;
        }
        if (searchResponse.isTimedOut()) {
            throw new IllegalStateException("search feedbacks from es timed out");
        }

        List<AssistantFeedback> rows = new ArrayList<>();
        for (SearchHit searchHit : searchResponse.getHits().getHits()) {
            rows.add(toFeedback(searchHit.getSourceAsMap()));
        }
        return PageResponse.of(rows, searchResponse.getHits().getTotalHits().value, pageNum, pageSize);
    }

    public int bulkUpsertFeedbacks(List<AssistantFeedbackEsDocument> documents) throws Exception {
        return bulkUpsertFeedbacksToIndex(documents, FEEDBACK_READ_ALIAS);
    }

    /**
     * 批量写入到指定索引（rebuildIndex 阶段写新物理索引时使用，绕开别名避免写到旧索引）。
     */
    public int bulkUpsertFeedbacksToIndex(List<AssistantFeedbackEsDocument> documents, String indexName) throws Exception {
        if (documents == null || documents.isEmpty()) {
            return 0;
        }
        BulkRequest bulkRequest = new BulkRequest();
        documents.forEach(document -> {
            try {
                bulkRequest.add(new IndexRequest(indexName)
                        .id(String.valueOf(document.getId()))
                        .source(writeDocument(document), XContentType.JSON));
            } catch (Exception e) {
                throw new RuntimeException("Failed to write document: " + document.getId(), e);
            }
        });
        BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (bulkResponse.hasFailures()) {
            throw new IllegalStateException("bulk upsert feedbacks to es failed: " + bulkResponse.buildFailureMessage());
        }
        return documents.size();
    }

    public void upsertFeedback(AssistantFeedbackEsDocument document) throws Exception {
        upsertFeedbackToIndex(document, FEEDBACK_READ_ALIAS);
    }

    public void upsertFeedbackToIndex(AssistantFeedbackEsDocument document, String indexName) throws Exception {
        if (document == null || document.getId() == null) {
            return;
        }
        IndexRequest indexRequest = new IndexRequest(indexName)
                .id(String.valueOf(document.getId()))
                .source(writeDocument(document), XContentType.JSON);
        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    }

    public void deleteFeedback(Long feedbackId) throws Exception {
        deleteFeedbackFromIndex(feedbackId, FEEDBACK_READ_ALIAS, true);
    }

    public void deleteFeedbackFromIndex(Long feedbackId, String indexName) throws Exception {
        deleteFeedbackFromIndex(feedbackId, indexName, false);
    }

    private void deleteFeedbackFromIndex(Long feedbackId, String indexName, boolean aliasMode) throws Exception {
        if (feedbackId == null || StrUtil.isBlank(indexName)) {
            return;
        }
        if (aliasMode && !aliasExists()) {
            return;
        }
        if (!aliasMode && !indexExists(indexName)) {
            return;
        }
        DeleteRequest deleteRequest = new DeleteRequest(indexName, String.valueOf(feedbackId));
        restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
    }

    /**
     * 清空当前别名指向的索引中的所有文档，但保留索引和 mapping。
     *
     * <p>定位：纯粹的"数据对齐"工具，用于修复某些原因导致的数据不一致（如事件丢失、
     * 同步失败补偿等），不触碰索引结构。与 {@link #rebuildIndex(String)} 的区别：
     * <ul>
     *   <li>本方法：保留 mapping，仅清空再 UPSERT，适合数据修复场景</li>
     *   <li>{@code rebuildIndex}：删旧索引、建新索引、切别名，适合 mapping 变更场景</li>
     * </ul>
     * </p>
     */
    public int deleteAllFeedbacks() throws Exception {
        if (!aliasExists()) {
            return 0;
        }
        Request request = new Request("POST", "/" + FEEDBACK_READ_ALIAS + "/_delete_by_query");
        request.addParameter("refresh", "true");
        request.addParameter("conflicts", "proceed");
        request.setJsonEntity("{\"query\":{\"match_all\":{}}}");
        Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
        String json = EntityUtils.toString(response.getEntity(), "UTF-8");
        JsonNode root = objectMapper.readTree(json);
        return root.path("deleted").asInt(0);
    }

    public void createPhysicalIndex(String targetIndexName, String indexDefinitionJson) throws Exception {
        if (StrUtil.isBlank(targetIndexName)) {
            throw new IllegalArgumentException("targetIndexName must not be blank");
        }
        if (indexExists(targetIndexName)) {
            throw new IllegalStateException("feedback physical index already exists: " + targetIndexName);
        }
        CreateIndexRequest createRequest = new CreateIndexRequest(targetIndexName);
        createRequest.source(indexDefinitionJson, XContentType.JSON);
        restHighLevelClient.indices().create(createRequest, RequestOptions.DEFAULT);
    }

    public void switchAlias(String oldIndexName, String targetIndexName) throws Exception {
        String addAliasJson;
        if (StrUtil.isBlank(targetIndexName)) {
            throw new IllegalArgumentException("targetIndexName must not be blank");
        }
        if (StrUtil.isNotBlank(oldIndexName) && !oldIndexName.equals(targetIndexName)) {
            // 原子操作：从旧索引移除别名 + 给新索引加别名
            addAliasJson = String.format(
                    "{\"actions\":[{\"remove\":{\"index\":\"%s\",\"alias\":\"%s\"}}," +
                            "{\"add\":{\"index\":\"%s\",\"alias\":\"%s\"}}]}",
                    oldIndexName, FEEDBACK_READ_ALIAS, targetIndexName, FEEDBACK_READ_ALIAS);
        } else {
            // 首次建索引，直接添加别名
            addAliasJson = String.format(
                    "{\"actions\":[{\"add\":{\"index\":\"%s\",\"alias\":\"%s\"}}]}",
                    targetIndexName, FEEDBACK_READ_ALIAS);
        }
        Request aliasRequest = new Request("POST", "/_aliases");
        aliasRequest.setJsonEntity(addAliasJson);
        restHighLevelClient.getLowLevelClient().performRequest(aliasRequest);
    }

    public void deletePhysicalIndex(String indexName) throws Exception {
        if (StrUtil.isBlank(indexName) || !indexExists(indexName)) {
            return;
        }
        restHighLevelClient.indices().delete(
                new org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest(indexName),
                RequestOptions.DEFAULT);
    }

    /**
     * 查询读别名当前指向的物理索引名。
     *
     * @return 物理索引名，别名不存在时返回 null
     */
    public String resolveCurrentPhysicalIndex() throws Exception {
        Request request = new Request("GET", "/_alias/" + FEEDBACK_READ_ALIAS);
        try {
            Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            JsonNode root = objectMapper.readTree(json);
            return root.fieldNames().hasNext() ? root.fieldNames().next() : null;
        } catch (Exception e) {
            // 别名不存在时 ES 返回 404
            return null;
        }
    }

    public boolean indexExists(String indexName) throws Exception {
        if (StrUtil.isBlank(indexName)) {
            return false;
        }
        return restHighLevelClient.indices().exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT);
    }

    public String buildNextPhysicalIndexName() {
        return FEEDBACK_INDEX_PREFIX + LocalDateTime.now().format(INDEX_SUFFIX_FORMATTER);
    }

    private SearchSourceBuilder buildSearchSource(AssistantFeedbackPageDTO dto,
                                                  int pageNum,
                                                  int pageSize,
                                                  List<Long> favoriteFeedbackIds) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .from((pageNum - 1) * pageSize)
                .size(pageSize)
                .timeout(TimeValue.timeValueMillis(searchEsProperties.getFallbackTimeoutMillis()));
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery("deleteFlag", 0))
                .mustNot(QueryBuilders.termQuery("categoryCode", "announcement"));

        if (favoriteFeedbackIds != null) {
            boolQuery.filter(QueryBuilders.termsQuery("id", favoriteFeedbackIds.stream()
                    .map(String::valueOf).collect(Collectors.toList())));
        }
        if (dto.getCategoryId() != null) {
            boolQuery.filter(QueryBuilders.termQuery("categoryId", String.valueOf(dto.getCategoryId())));
        }
        if (StrUtil.isNotBlank(dto.getCategoryCode())) {
            boolQuery.filter(QueryBuilders.termQuery("categoryCode", dto.getCategoryCode().trim()));
        }
        if (StrUtil.isNotBlank(dto.getStatus())) {
            boolQuery.filter(QueryBuilders.termQuery("status", AssistantTicketStatus.normalize(dto.getStatus())));
        }
        if (dto.getIsPinned() != null) {
            boolQuery.filter(QueryBuilders.termQuery("isPinned", dto.getIsPinned()));
        }
        if (QUERY_MODE_MINE.equals(normalizeQueryMode(dto.getQueryMode())) && dto.getUserId() != null) {
            boolQuery.filter(QueryBuilders.termQuery("userId", String.valueOf(dto.getUserId())));
        }
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("tagIds", dto.getTagIds().stream()
                    .map(String::valueOf).collect(Collectors.toList())));
        }
        if (StrUtil.isNotBlank(dto.getKeyword())) {
            boolQuery.must(buildKeywordQuery(dto.getKeyword().trim()));
        }

        sourceBuilder.query(boolQuery);
        appendSorts(sourceBuilder, dto);
        return sourceBuilder;
    }

    private void appendSorts(SearchSourceBuilder sourceBuilder, AssistantFeedbackPageDTO dto) {
        boolean searchMode = StrUtil.isNotBlank(dto.getKeyword());
        if (!searchMode && QUERY_MODE_MINE.equals(normalizeQueryMode(dto.getQueryMode()))) {
            sourceBuilder.sort(SortBuilders.fieldSort("mineStatusPriority").order(SortOrder.ASC).missing("_last").unmappedType("integer"));
        }
        if (!searchMode) {
            sourceBuilder.sort(SortBuilders.fieldSort("isPinned").order(SortOrder.DESC).missing("_last").unmappedType("integer"));
            sourceBuilder.sort(SortBuilders.fieldSort("pinOrder").order(SortOrder.ASC).missing("_last").unmappedType("integer"));
        }

        String sortType = resolveSortType(dto.getSortType(), searchMode);
        if (SORT_TYPE_RELATED.equals(sortType)) {
            sourceBuilder.sort(SortBuilders.scoreSort().order(SortOrder.DESC));
            sourceBuilder.sort(SortBuilders.fieldSort("hotScore").order(SortOrder.DESC).missing("_last").unmappedType("integer"));
            sourceBuilder.sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC).missing("_last").unmappedType("date"));
            return;
        }
        if ("comment".equals(sortType)) {
            sourceBuilder.sort(SortBuilders.fieldSort("commentCount").order(SortOrder.DESC).missing("_last").unmappedType("integer"));
            sourceBuilder.sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC).missing("_last").unmappedType("date"));
            return;
        }
        if ("latest".equals(sortType)) {
            sourceBuilder.sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC).missing("_last").unmappedType("date"));
            return;
        }
        sourceBuilder.sort(SortBuilders.fieldSort("hotScore").order(SortOrder.DESC).missing("_last").unmappedType("integer"));
        sourceBuilder.sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC).missing("_last").unmappedType("date"));
    }

    private String resolveSortType(String sortType, boolean searchMode) {
        String resolvedSortType = StrUtil.isBlank(sortType) ? DEFAULT_SORT_TYPE : sortType.trim().toLowerCase();
        if (SORT_TYPE_RELATED.equals(resolvedSortType) && !searchMode) {
            return DEFAULT_SORT_TYPE;
        }
        return resolvedSortType;
    }

    private QueryBuilder buildKeywordQuery(String keyword) {
        return QueryBuilders.multiMatchQuery(keyword)
                .field("title", 8.0f)
                .field("content", 4.0f)
                .field("ticketNo", 2.0f)
                .type(MultiMatchQueryBuilder.Type.BEST_FIELDS);
    }

    private AssistantFeedback toFeedback(Map<String, Object> source) {
        AssistantFeedbackEsDocument document = objectMapper.convertValue(source, AssistantFeedbackEsDocument.class);
        fillTimeFields(document, source);
        AssistantFeedback feedback = new AssistantFeedback();
        feedback.setId(document.getId());
        feedback.setUserId(document.getUserId());
        feedback.setCategoryId(document.getCategoryId());
        feedback.setTicketNo(document.getTicketNo());
        feedback.setTitle(document.getTitle());
        feedback.setContent(document.getContent());
        feedback.setStatus(document.getStatus());
        feedback.setIsPinned(document.getIsPinned());
        feedback.setPinOrder(document.getPinOrder());
        feedback.setCommentCount(document.getCommentCount());
        feedback.setViewCount(document.getViewCount());
        feedback.setLikeCount(document.getLikeCount());
        feedback.setFavoriteCount(document.getFavoriteCount());
        feedback.setHotScore(document.getHotScore());
        feedback.setCreateTime(document.getCreateTime());
        feedback.setUpdateTime(document.getUpdateTime());
        feedback.setDeleteFlag(document.getDeleteFlag() == null ? (byte) 0 : document.getDeleteFlag().byteValue());
        return feedback;
    }

    private void fillTimeFields(AssistantFeedbackEsDocument document, Map<String, Object> source) {
        if (document.getCreateTime() == null) {
            document.setCreateTime(parseLocalDateTime(source.get("createTime")));
        }
        if (document.getUpdateTime() == null) {
            document.setUpdateTime(parseLocalDateTime(source.get("updateTime")));
        }
    }

    private LocalDateTime parseLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(((Number) value).longValue()), ZoneId.systemDefault());
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception ignored) {
            return LocalDateTime.parse(text, new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd HH:mm:ss")
                    .optionalStart()
                    .appendFraction(ChronoField.MILLI_OF_SECOND, 1, 3, true)
                    .optionalEnd()
                    .toFormatter());
        }
    }

    private String normalizeQueryMode(String queryMode) {
        if (StrUtil.isBlank(queryMode)) {
            return "all";
        }
        return queryMode.trim().toLowerCase();
    }

    private int resolvePageNum(AssistantFeedbackPageDTO dto) {
        if (dto == null || dto.getPageNum() == null || dto.getPageNum() < 1) {
            return 1;
        }
        return dto.getPageNum();
    }

    private int resolvePageSize(AssistantFeedbackPageDTO dto) {
        if (dto == null || dto.getPageSize() == null || dto.getPageSize() < 1) {
            return 10;
        }
        return dto.getPageSize();
    }

    private boolean aliasExists() throws Exception {
        Request request = new Request("HEAD", "/_alias/" + FEEDBACK_READ_ALIAS);
        try {
            Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
            return response.getStatusLine().getStatusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private void ensureAliasExists() throws Exception {
        if (!aliasExists()) {
            throw new IllegalStateException("feedback search alias missing: " + FEEDBACK_READ_ALIAS);
        }
    }

    public static String loadMappingJson() throws Exception {
        ClassPathResource resource = new ClassPathResource(MAPPING_RESOURCE);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    private String writeDocument(AssistantFeedbackEsDocument document) throws Exception {
        return objectMapper.writeValueAsString(document);
    }
}

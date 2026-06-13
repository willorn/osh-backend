package com.backstage.system.mapper.tool;

import com.backstage.common.response.PageResponse;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.config.properties.SearchEsProperties;
import com.backstage.system.domain.tool.OshTool;
import com.backstage.system.request.tool.ToolSearchRequest;
import com.backstage.system.service.tool.ToolIndexMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OshToolEsMapper {

    private static final String TOOL_SEARCH_INDEX = "osh_tool_search";

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SearchEsProperties searchEsProperties;

    public PageResponse<OshTool> searchTools(ToolSearchRequest request, List<Long> toolIds) throws Exception {
        int pageNum = request.getPageNum();
        int pageSize = request.getPageSize();
        if (toolIds != null && toolIds.isEmpty()) {
            return PageResponse.of(new ArrayList<>(), 0L, pageNum, pageSize);
        }
        if (!restHighLevelClient.indices().exists(new GetIndexRequest(TOOL_SEARCH_INDEX), RequestOptions.DEFAULT)) {
            return PageResponse.of(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        SearchRequest searchRequest = new SearchRequest(TOOL_SEARCH_INDEX);
        searchRequest.source(buildSearchSource(request, pageNum, pageSize, toolIds));
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
            throw new IllegalStateException("search tools from es timed out");
        }

        List<OshTool> rows = new ArrayList<>();
        for (SearchHit searchHit : searchResponse.getHits().getHits()) {
            Map<String, Object> source = searchHit.getSourceAsMap();
            Map<String, Object> convertSource = new HashMap<>(source);
            convertSource.remove("createTime");
            convertSource.remove("updateTime");
            ToolIndexMessage document = objectMapper.convertValue(convertSource, ToolIndexMessage.class);
            fillTimeFields(document, source);
            rows.add(toTool(document));
        }
        return PageResponse.of(rows, searchResponse.getHits().getTotalHits().value, pageNum, pageSize);
    }

    public int bulkUpsertTools(List<ToolIndexMessage> documents) throws Exception {
        if (StringUtils.isEmpty(documents)) {
            return 0;
        }
        BulkRequest bulkRequest = new BulkRequest();
        for (ToolIndexMessage document : documents) {
            Map<String, Object> source = objectMapper.convertValue(document, Map.class);
            source.remove("eventType");
            bulkRequest.add(new IndexRequest(TOOL_SEARCH_INDEX)
                    .id(String.valueOf(document.getId()))
                    .source(objectMapper.writeValueAsString(source), XContentType.JSON));
        }
        BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (bulkResponse.hasFailures()) {
            throw new IllegalStateException("bulk upsert tools to es failed: " + bulkResponse.buildFailureMessage());
        }
        return documents.size();
    }

    public int deleteAllTools() throws Exception {
        GetIndexRequest getIndexRequest = new GetIndexRequest(TOOL_SEARCH_INDEX);
        if (!restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT)) {
            return 0;
        }
        Request request = new Request("POST", "/" + TOOL_SEARCH_INDEX + "/_delete_by_query");
        request.addParameter("refresh", "true");
        request.addParameter("conflicts", "proceed");
        request.setJsonEntity("{\"query\":{\"match_all\":{}}}");

        Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
        String json = EntityUtils.toString(response.getEntity(), "UTF-8");
        JsonNode root = objectMapper.readTree(json);
        return root.path("deleted").asInt(0);
    }

    private SearchSourceBuilder buildSearchSource(ToolSearchRequest request, int pageNum, int pageSize, List<Long> toolIds) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .from((pageNum - 1) * pageSize)
                .size(pageSize)
                .timeout(TimeValue.timeValueMillis(searchEsProperties.getFallbackTimeoutMillis()));
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery("status", 4))
                .filter(QueryBuilders.termQuery("deleteFlag", 0));

        if (toolIds != null && !toolIds.isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("id", toolIds));
        }
        if (request.getToolId() != null) {
            boolQuery.filter(QueryBuilders.termQuery("id", request.getToolId()));
        }
        if (StringUtils.isNotEmpty(request.getNo())) {
            boolQuery.filter(QueryBuilders.termQuery("no", request.getNo().trim()));
        }
        if (StringUtils.isNotEmpty(request.getKeyword())) {
            boolQuery.must(buildKeywordQuery(request.getKeyword()));
            sourceBuilder.sort(SortBuilders.scoreSort().order(SortOrder.DESC));
        }
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("tagIds", request.getTags()));
        }
        if (StringUtils.isNotEmpty(request.getResourceType())) {
            boolQuery.filter(QueryBuilders.termQuery("resourceType", request.getResourceType()));
        }

        sourceBuilder.query(boolQuery);
        sourceBuilder.sort(SortBuilders.fieldSort("hotScore").order(SortOrder.DESC));
        sourceBuilder.sort(SortBuilders.fieldSort("viewCount").order(SortOrder.DESC));
        sourceBuilder.sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC));
        return sourceBuilder;
    }

    private QueryBuilder buildKeywordQuery(String keyword) {
        return QueryBuilders.multiMatchQuery(keyword)
                .field("toolName", 8.0f)
                .field("tagNamesText", 4.0f)
                .field("description", 3.0f)
                .field("searchText", 1.0f)
                .type(MultiMatchQueryBuilder.Type.BEST_FIELDS);
    }

    private OshTool toTool(ToolIndexMessage document) {
        OshTool tool = new OshTool();
        tool.setId(document.getId());
        tool.setToolName(document.getToolName());
        tool.setNo(document.getNo());
        tool.setDescription(document.getDescription());
        tool.setRoutePath(document.getRoutePath());
        tool.setGithubUrl(document.getGithubUrl());
        tool.setResourceType(document.getResourceType());
        tool.setQuotaCost(document.getQuotaCost());
        tool.setLevel(document.getLevel());
        tool.setStatus(document.getStatus());
        tool.setDeleteFlag(document.getDeleteFlag());
        tool.setTags(document.getTagNames());
        tool.setViewCount(document.getViewCount());
        tool.setTotalUsage(document.getTotalUsage());
        tool.setCollectionCount(document.getCollectionCount());
        tool.setGoodCount(document.getGoodCount());
        tool.setBadCount(document.getBadCount());
        tool.setCreateBy(document.getCreateBy());
        tool.setUpdateBy(document.getUpdateBy());
        tool.setCreateTime(document.getCreateTime());
        tool.setUpdateTime(document.getUpdateTime());
        return tool;
    }

    private void fillTimeFields(ToolIndexMessage document, Map<String, Object> source) {
        if (document.getCreateTime() == null) {
            document.setCreateTime(parseLocalDateTime(source.get("createTime")));
        }
        if (document.getUpdateTime() == null) {
            document.setUpdateTime(parseLocalDateTime(source.get("updateTime")));
        }
    }

    private java.time.LocalDateTime parseLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(((Number) value).longValue()),
                    java.time.ZoneId.systemDefault());
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return java.time.LocalDateTime.parse(text, java.time.format.DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception ignored) {
            return java.time.LocalDateTime.parse(text, new java.time.format.DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd HH:mm:ss")
                    .optionalStart()
                    .appendFraction(java.time.temporal.ChronoField.MILLI_OF_SECOND, 1, 3, true)
                    .optionalEnd()
                    .toFormatter());
        }
    }
}

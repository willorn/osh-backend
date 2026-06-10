package com.backstage.system.mapper.info_gap;

import com.backstage.common.utils.StringUtils;
import com.backstage.system.config.properties.SearchEsProperties;
import com.backstage.system.domain.dto.info_gap.InfoGapESSearchReqDTO;
import com.backstage.system.domain.dto.info_gap.InfoGapSearchReqDTO;
import com.backstage.system.domain.info_gap.OshInfoGapEsDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.util.EntityUtils;
import org.apache.ibatis.annotations.Mapper;
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
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class OshInfoGapEsMapper {

    private static final String INFO_GAP_SEARCH_INDEX = "osh_infogap_search_index";
    private static final int RERANK_MULTIPLIER = 20;
    private static final int MIN_RERANK_WINDOW = 200;
    private static final int MAX_RERANK_WINDOW = 1000;

    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SearchEsProperties searchEsProperties;

    public InfoGapEsSearchResult searchInfoGaps(InfoGapESSearchReqDTO request) throws Exception {
        int pageNum = Optional.ofNullable(request.getPageNum()).orElse(1);
        int pageSize = Optional.ofNullable(request.getPageSize()).orElse(10);

        if (!restHighLevelClient.indices().exists(new GetIndexRequest(INFO_GAP_SEARCH_INDEX), RequestOptions.DEFAULT)) {
            return new InfoGapEsSearchResult(Collections.emptyList(), 0L, pageNum, pageSize);
        }

        SearchRequest searchRequest = new SearchRequest(INFO_GAP_SEARCH_INDEX);
        int fetchSize = resolveRerankWindow(pageNum, pageSize);
        searchRequest.source(buildSearchSource(request, 1, fetchSize));

        SearchResponse searchResponse;
        try {
            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException ex) {
            if (ex.status() != null && ex.status().getStatus() == 404) {
                return new InfoGapEsSearchResult(Collections.emptyList(), 0L, pageNum, pageSize);
            }
            throw ex;
        }

        if (searchResponse.isTimedOut()) {
            throw new IllegalStateException("search info gaps from es timed out");
        }

        List<Long> ids = buildRerankedIds(searchResponse.getHits().getHits(), request.getKeyword(), pageNum, pageSize);

        return new InfoGapEsSearchResult(ids, searchResponse.getHits().getTotalHits().value, pageNum, pageSize);
    }

    public InfoGapEsSearchResult searchInfoGaps(InfoGapSearchReqDTO request) throws Exception {
        int pageNum = Optional.ofNullable(request.getPageNum()).orElse(1);
        int pageSize = Optional.ofNullable(request.getPageSize()).orElse(10);

        if (!restHighLevelClient.indices().exists(new GetIndexRequest(INFO_GAP_SEARCH_INDEX), RequestOptions.DEFAULT)) {
            return new InfoGapEsSearchResult(Collections.emptyList(), 0L, pageNum, pageSize);
        }

        SearchRequest searchRequest = new SearchRequest(INFO_GAP_SEARCH_INDEX);
        int fetchSize = resolveRerankWindow(pageNum, pageSize);
        searchRequest.source(buildSearchSource(request, 1, fetchSize));

        SearchResponse searchResponse;
        try {
            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException ex) {
            if (ex.status() != null && ex.status().getStatus() == 404) {
                return new InfoGapEsSearchResult(Collections.emptyList(), 0L, pageNum, pageSize);
            }
            throw ex;
        }

        if (searchResponse.isTimedOut()) {
            throw new IllegalStateException("search info gaps from es timed out");
        }

        List<Long> ids = buildRerankedIds(searchResponse.getHits().getHits(), request.getKeyword(), pageNum, pageSize);

        return new InfoGapEsSearchResult(ids, searchResponse.getHits().getTotalHits().value, pageNum, pageSize);
    }

    public int bulkUpsertInfoGaps(List<OshInfoGapEsDocument> documents) throws Exception {
        if (documents == null || documents.isEmpty()) {
            return 0;
        }

        BulkRequest bulkRequest = new BulkRequest();
        for (OshInfoGapEsDocument document : documents) {
            bulkRequest.add(new IndexRequest(INFO_GAP_SEARCH_INDEX)
                    .id(String.valueOf(document.getId()))
                    .source(objectMapper.writeValueAsString(document), XContentType.JSON));
        }

        BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (bulkResponse.hasFailures()) {
            throw new IllegalStateException("bulk upsert info gaps to es failed: " + bulkResponse.buildFailureMessage());
        }
        return documents.size();
    }

    public void upsertInfoGap(OshInfoGapEsDocument document) throws Exception {
        IndexRequest indexRequest = new IndexRequest(INFO_GAP_SEARCH_INDEX)
                .id(String.valueOf(document.getId()))
                .source(objectMapper.writeValueAsString(document), XContentType.JSON);
        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    }

    public void deleteInfoGap(Long infoGapId) throws Exception {
        GetIndexRequest getIndexRequest = new GetIndexRequest(INFO_GAP_SEARCH_INDEX);
        if (!restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT)) {
            return;
        }
        DeleteRequest deleteRequest = new DeleteRequest(INFO_GAP_SEARCH_INDEX, String.valueOf(infoGapId));
        restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
    }

    public int deleteAllInfoGaps() throws Exception {
        GetIndexRequest getIndexRequest = new GetIndexRequest(INFO_GAP_SEARCH_INDEX);
        if (!restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT)) {
            return 0;
        }

        Request request = new Request("POST", "/" + INFO_GAP_SEARCH_INDEX + "/_delete_by_query");
        request.addParameter("refresh", "true");
        request.addParameter("conflicts", "proceed");
        request.setJsonEntity("{\"query\":{\"match_all\":{}}}");

        Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
        String json = EntityUtils.toString(response.getEntity(), "UTF-8");
        JsonNode root = objectMapper.readTree(json);
        return root.path("deleted").asInt(0);
    }

    public boolean indexExists() throws Exception {
        return restHighLevelClient.indices().exists(new GetIndexRequest(INFO_GAP_SEARCH_INDEX), RequestOptions.DEFAULT);
    }

    private SearchSourceBuilder buildSearchSource(InfoGapESSearchReqDTO request, int pageNum, int pageSize) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .from((pageNum - 1) * pageSize)
                .size(pageSize)
                .timeout(TimeValue.timeValueMillis(searchEsProperties.getFallbackTimeoutMillis()));

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery("status", 4))
                .filter(QueryBuilders.termQuery("deleteFlag", 0));

        if (request != null && StringUtils.isNotEmpty(request.getKeyword())) {
            boolQuery.must(buildKeywordQuery(request.getKeyword()));
            sourceBuilder.sort(SortBuilders.scoreSort().order(SortOrder.DESC));
        }

        sourceBuilder.query(boolQuery);
        sourceBuilder.sort(SortBuilders.fieldSort("updateTime").order(SortOrder.DESC).unmappedType("date"));
        return sourceBuilder;
    }

    private SearchSourceBuilder buildSearchSource(InfoGapSearchReqDTO request, int pageNum, int pageSize) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .from((pageNum - 1) * pageSize)
                .size(pageSize)
                .timeout(TimeValue.timeValueMillis(searchEsProperties.getFallbackTimeoutMillis()));

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery("status", 4))
                .filter(QueryBuilders.termQuery("deleteFlag", 0));

        if (request != null && StringUtils.isNotEmpty(request.getKeyword())) {
            boolQuery.must(buildKeywordQuery(request.getKeyword()));
            sourceBuilder.sort(SortBuilders.scoreSort().order(SortOrder.DESC));
        }

        if (request != null && request.getTagId() != null) {
            boolQuery.filter(QueryBuilders.termQuery("tagIds", request.getTagId()));
        }

        if (request != null && StringUtils.isNotEmpty(request.getCategory())) {
            boolQuery.filter(QueryBuilders.termQuery("category", request.getCategory()));
        }

        sourceBuilder.query(boolQuery);
        sourceBuilder.sort(SortBuilders.fieldSort("updateTime").order(SortOrder.DESC).unmappedType("date"));
        return sourceBuilder;
    }

    private QueryBuilder buildKeywordQuery(String keyword) {
        String trimmedKeyword = keyword == null ? null : keyword.trim();
        BoolQueryBuilder shouldQuery = QueryBuilders.boolQuery();
        shouldQuery.should(QueryBuilders.matchQuery("title", trimmedKeyword).boost(20.0f));
        shouldQuery.should(QueryBuilders.matchQuery("content", trimmedKeyword).boost(18.0f));
        shouldQuery.should(QueryBuilders.matchQuery("tagNamesText", trimmedKeyword).boost(3.0f));
        shouldQuery.should(QueryBuilders.matchQuery("category", trimmedKeyword).boost(1.0f));
        shouldQuery.minimumShouldMatch(1);
        return shouldQuery;
    }

    private int resolveRerankWindow(int pageNum, int pageSize) {
        int base = Math.max(1, pageNum) * Math.max(1, pageSize) * RERANK_MULTIPLIER;
        base = Math.max(base, MIN_RERANK_WINDOW);
        return Math.min(base, MAX_RERANK_WINDOW);
    }

    private List<Long> buildRerankedIds(SearchHit[] hits, String keyword, int pageNum, int pageSize) {
        if (hits == null || hits.length == 0) {
            return Collections.emptyList();
        }
        String normalizedKeyword = normalizeKeyword(keyword);
        List<ScoredHit> rankedHits = new ArrayList<>(hits.length);
        for (SearchHit hit : hits) {
            long frequencyScore = calculateFrequencyScore(hit, normalizedKeyword);
            String updateTime = extractField(hit, "updateTime");
            rankedHits.add(new ScoredHit(Long.valueOf(hit.getId()), frequencyScore, hit.getScore(), updateTime));
        }
        rankedHits.sort(Comparator
                .comparingLong(ScoredHit::getFrequencyScore).reversed()
                .thenComparing(ScoredHit::getEsScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ScoredHit::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())));

        int fromIndex = Math.max(0, (pageNum - 1) * pageSize);
        if (fromIndex >= rankedHits.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(rankedHits.size(), fromIndex + pageSize);
        List<Long> ids = new ArrayList<>(toIndex - fromIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            ids.add(rankedHits.get(i).getId());
        }
        return ids;
    }

    private long calculateFrequencyScore(SearchHit hit, String keyword) {
        if (StringUtils.isEmpty(keyword)) {
            return 0L;
        }
        long total = 0L;
        total += countOccurrences(extractField(hit, "title"), keyword) * 100L;
        total += countOccurrences(extractField(hit, "content"), keyword) * 80L;
        total += countOccurrences(extractField(hit, "tagNamesText"), keyword) * 20L;
        total += countOccurrences(extractField(hit, "category"), keyword) * 10L;
        return total;
    }

    private String extractField(SearchHit hit, String fieldName) {
        Object value = hit.getSourceAsMap() == null ? null : hit.getSourceAsMap().get(fieldName);
        return value == null ? "" : String.valueOf(value);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase();
    }

    private int countOccurrences(String text, String keyword) {
        if (StringUtils.isEmpty(text) || StringUtils.isEmpty(keyword)) {
            return 0;
        }
        String source = text.toLowerCase();
        int count = 0;
        int fromIndex = 0;
        while (true) {
            int matchIndex = source.indexOf(keyword, fromIndex);
            if (matchIndex < 0) {
                break;
            }
            count++;
            fromIndex = matchIndex + keyword.length();
        }
        return count;
    }

    private static class ScoredHit {
        private final Long id;
        private final long frequencyScore;
        private final Float esScore;
        private final String updateTime;

        private ScoredHit(Long id, long frequencyScore, Float esScore, String updateTime) {
            this.id = id;
            this.frequencyScore = frequencyScore;
            this.esScore = esScore;
            this.updateTime = updateTime;
        }

        public Long getId() {
            return id;
        }

        public long getFrequencyScore() {
            return frequencyScore;
        }

        public Float getEsScore() {
            return esScore;
        }

        public String getUpdateTime() {
            return updateTime;
        }
    }

    public static class InfoGapEsSearchResult {
        private final List<Long> ids;
        private final long total;
        private final int pageNum;
        private final int pageSize;

        public InfoGapEsSearchResult(List<Long> ids, long total, int pageNum, int pageSize) {
            this.ids = ids;
            this.total = total;
            this.pageNum = pageNum;
            this.pageSize = pageSize;
        }

        public List<Long> getIds() {
            return ids;
        }

        public long getTotal() {
            return total;
        }

        public int getPageNum() {
            return pageNum;
        }

        public int getPageSize() {
            return pageSize;
        }
    }
}

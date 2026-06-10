package com.backstage.system.mapper.book;

import com.backstage.common.utils.StringUtils;
import com.backstage.system.config.properties.SearchEsProperties;
import com.backstage.system.controller.book.BookListReqVO;
import com.backstage.system.domain.book.es.OshBookEsDocument;
import com.backstage.system.domain.vo.book.BookListVO;
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
import java.util.List;
import java.util.Optional;

@Component
public class BookEsMapper {

    private static final String BOOK_SEARCH_INDEX = "osh_book_search_read";

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SearchEsProperties searchEsProperties;

    public BookEsSearchResult searchBooks(BookListReqVO request) throws Exception {
        int pageNum = Optional.ofNullable(request.getPageNum()).orElse(1L).intValue();
        int pageSize = Optional.ofNullable(request.getPageSize()).orElse(12L).intValue();

        if (!restHighLevelClient.indices().exists(new GetIndexRequest(BOOK_SEARCH_INDEX), RequestOptions.DEFAULT)) {
            return new BookEsSearchResult(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        SearchRequest searchRequest = new SearchRequest(BOOK_SEARCH_INDEX);
        searchRequest.source(buildSearchSource(request, pageNum, pageSize));
        SearchResponse searchResponse;
        try {
            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException ex) {
            if (ex.status() != null && ex.status().getStatus() == 404) {
                return new BookEsSearchResult(new ArrayList<>(), 0L, pageNum, pageSize);
            }
            throw ex;
        }
        if (searchResponse.isTimedOut()) {
            throw new IllegalStateException("search books from es timed out");
        }

        List<BookListVO> rows = new ArrayList<>();
        for (SearchHit searchHit : searchResponse.getHits().getHits()) {
            OshBookEsDocument document = objectMapper.convertValue(searchHit.getSourceAsMap(), OshBookEsDocument.class);
            rows.add(toVo(document));
        }

        return new BookEsSearchResult(rows, searchResponse.getHits().getTotalHits().value, pageNum, pageSize);
    }

    public int bulkUpsertBooks(List<OshBookEsDocument> documents) throws Exception {
        if (documents == null || documents.isEmpty()) {
            return 0;
        }

        BulkRequest bulkRequest = new BulkRequest();
        for (OshBookEsDocument document : documents) {
            bulkRequest.add(new IndexRequest(BOOK_SEARCH_INDEX)
                    .id(String.valueOf(document.getId()))
                    .source(objectMapper.writeValueAsString(document), XContentType.JSON));
        }

        BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (bulkResponse.hasFailures()) {
            throw new IllegalStateException("bulk upsert books to es failed: " + bulkResponse.buildFailureMessage());
        }
        return documents.size();
    }

    public void upsertBook(OshBookEsDocument document) throws Exception {
        IndexRequest indexRequest = new IndexRequest(BOOK_SEARCH_INDEX)
                .id(String.valueOf(document.getId()))
                .source(objectMapper.writeValueAsString(document), XContentType.JSON);
        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    }

    public void deleteBook(Long bookId) throws Exception {
        GetIndexRequest getIndexRequest = new GetIndexRequest(BOOK_SEARCH_INDEX);
        if (!restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT)) {
            return;
        }
        org.elasticsearch.action.delete.DeleteRequest deleteRequest =
                new org.elasticsearch.action.delete.DeleteRequest(BOOK_SEARCH_INDEX, String.valueOf(bookId));
        restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
    }

    public int deleteAllBooks() throws Exception {
        GetIndexRequest getIndexRequest = new GetIndexRequest(BOOK_SEARCH_INDEX);
        if (!restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT)) {
            return 0;
        }

        Request request = new Request("POST", "/" + BOOK_SEARCH_INDEX + "/_delete_by_query");
        request.addParameter("refresh", "true");
        request.addParameter("conflicts", "proceed");
        request.setJsonEntity("{\"query\":{\"match_all\":{}}}");

        Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
        String json = EntityUtils.toString(response.getEntity(), "UTF-8");
        JsonNode root = objectMapper.readTree(json);
        return root.path("deleted").asInt(0);
    }

    private SearchSourceBuilder buildSearchSource(BookListReqVO request, int pageNum, int pageSize) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .from((pageNum - 1) * pageSize)
                .size(pageSize)
                .timeout(TimeValue.timeValueMillis(searchEsProperties.getFallbackTimeoutMillis()));

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery("status", "0"))
                .filter(QueryBuilders.termQuery("deleteFlag", 0));

        if (request != null && StringUtils.isNotEmpty(request.getTitle())) {
            boolQuery.must(buildKeywordQuery(request.getTitle()));
            sourceBuilder.sort(SortBuilders.scoreSort().order(SortOrder.DESC));
        }

        if (request != null && StringUtils.isNotEmpty(request.getTagNameList())) {
            boolQuery.filter(QueryBuilders.termsQuery("tagNames", request.getTagNameList()));
        }

        if (request != null && request.getLevel() != null) {
            boolQuery.filter(QueryBuilders.termQuery("level", request.getLevel()));
            if (request.getUserLevel() != null) {
                boolQuery.filter(QueryBuilders.rangeQuery("level").lte(request.getUserLevel()));
            }
        } else if (request != null && request.getUserLevel() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("level").lte(request.getUserLevel()));
        }

        sourceBuilder.query(boolQuery);
        sourceBuilder.sort(SortBuilders.fieldSort("subCount").order(SortOrder.DESC).missing("_last").unmappedType("integer"));
        sourceBuilder.sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC).unmappedType("date"));
        return sourceBuilder;
    }

    private QueryBuilder buildKeywordQuery(String keyword) {
        return QueryBuilders.multiMatchQuery(keyword)
                .field("title", 8.0f)
                .field("tagNamesText", 4.0f)
                .field("description", 3.0f)
                .field("searchText", 1.0f)
                .type(MultiMatchQueryBuilder.Type.BEST_FIELDS);
    }

    private BookListVO toVo(OshBookEsDocument document) {
        BookListVO vo = new BookListVO();
        vo.setId(document.getId());
        vo.setTitle(document.getTitle());
        vo.setCover(document.getCover());
        vo.setDesc(document.getDescription());
        vo.setPrice(document.getPrice() != null ? document.getPrice().toPlainString() : "0");
        vo.setTPrice(document.getOriginalPrice() != null ? document.getOriginalPrice().toPlainString() : "0");
        vo.setSubCount(document.getSubCount());
        vo.setChapterCount(document.getChapterCount());
        vo.setLevel(document.getLevel());
        vo.setTagNameList(document.getTagNames());
        vo.setPurchaseCount(0);
        return vo;
    }

    public static class BookEsSearchResult {
        private final List<BookListVO> rows;
        private final long total;
        private final int pageNum;
        private final int pageSize;

        public BookEsSearchResult(List<BookListVO> rows, long total, int pageNum, int pageSize) {
            this.rows = rows;
            this.total = total;
            this.pageNum = pageNum;
            this.pageSize = pageSize;
        }

        public List<BookListVO> getRows() { return rows; }
        public long getTotal() { return total; }
        public int getPageNum() { return pageNum; }
        public int getPageSize() { return pageSize; }
    }
}

package com.backstage.system.mapper.course;

import com.backstage.common.response.PageResponse;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.config.properties.SearchEsProperties;
import com.backstage.system.domain.course.es.OshCourseEsDocument;
import com.backstage.system.domain.course.vo.CourseSearchLoginVo;
import com.fasterxml.jackson.databind.JsonNode;
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
import com.backstage.system.request.CourseSearchRequest;
import org.apache.http.util.EntityUtils;
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

@Component
public class OshCourseEsMapper {

    private static final String COURSE_SEARCH_INDEX = "osh_course_search_read";

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SearchEsProperties searchEsProperties;

    public PageResponse<CourseSearchLoginVo> searchCourses(CourseSearchRequest request) throws Exception {
        return searchCourses(request, null);
    }

    public PageResponse<CourseSearchLoginVo> searchCourses(CourseSearchRequest request, List<Long> courseIds) throws Exception {
        int pageNum = request.getPageNum();
        int pageSize = request.getPageSize();

        if (courseIds != null && courseIds.isEmpty()) {
            return PageResponse.of(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        if (!restHighLevelClient.indices().exists(new GetIndexRequest(COURSE_SEARCH_INDEX), RequestOptions.DEFAULT)) {
            return PageResponse.of(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        SearchRequest searchRequest = new SearchRequest(COURSE_SEARCH_INDEX);
        searchRequest.source(buildSearchSource(request, pageNum, pageSize, courseIds));
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
            throw new IllegalStateException("search courses from es timed out");
        }
        List<CourseSearchLoginVo> rows = new ArrayList<>();
        for (SearchHit searchHit : searchResponse.getHits().getHits()) {
            OshCourseEsDocument document = objectMapper.convertValue(searchHit.getSourceAsMap(), OshCourseEsDocument.class);
            rows.add(toVo(document));
        }

        return PageResponse.of(rows, searchResponse.getHits().getTotalHits().value, pageNum, pageSize);
    }

    public int bulkUpsertCourses(List<OshCourseEsDocument> documents) throws Exception {
        if (StringUtils.isEmpty(documents)) {
            return 0;
        }

        BulkRequest bulkRequest = new BulkRequest();
        for (OshCourseEsDocument document : documents) {
            bulkRequest.add(new IndexRequest(COURSE_SEARCH_INDEX)
                    .id(String.valueOf(document.getId()))
                    .source(objectMapper.writeValueAsString(document), XContentType.JSON));
        }

        BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (bulkResponse.hasFailures()) {
            throw new IllegalStateException("bulk upsert courses to es failed: " + bulkResponse.buildFailureMessage());
        }
        return documents.size();
    }

    public int deleteAllCourses() throws Exception {
        GetIndexRequest getIndexRequest = new GetIndexRequest(COURSE_SEARCH_INDEX);
        if (!restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT)) {
            return 0;
        }

        Request request = new Request("POST", "/" + COURSE_SEARCH_INDEX + "/_delete_by_query");
        request.addParameter("refresh", "true");
        request.addParameter("conflicts", "proceed");
        request.setJsonEntity("{\"query\":{\"match_all\":{}}}");

        Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
        String json = EntityUtils.toString(response.getEntity(), "UTF-8");
        JsonNode root = objectMapper.readTree(json);
        return root.path("deleted").asInt(0);
    }

    public void recreateCourseSearchIndex(String indexDefinitionJson) throws Exception {
        GetIndexRequest getIndexRequest = new GetIndexRequest(COURSE_SEARCH_INDEX);
        boolean exists = restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
        if (exists) {
            DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(COURSE_SEARCH_INDEX);
            restHighLevelClient.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
        }

        CreateIndexRequest createIndexRequest = new CreateIndexRequest(COURSE_SEARCH_INDEX);
        createIndexRequest.source(indexDefinitionJson, XContentType.JSON);
        restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
    }

    private SearchSourceBuilder buildSearchSource(CourseSearchRequest request, int pageNum, int pageSize, List<Long> courseIds) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .from((pageNum - 1) * pageSize)
                .size(pageSize)
                .timeout(TimeValue.timeValueMillis(searchEsProperties.getFallbackTimeoutMillis()));
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery("deleteFlag", 0));
        if (request == null || request.getIncludeUnpublished() == null || !request.getIncludeUnpublished()) {
            boolQuery.filter(QueryBuilders.termQuery("status", 4));
        }
        // 已隐藏课程（status=7）：仅「已隐藏课程」分类返回，其余视图一律排除
        if (request != null && Boolean.TRUE.equals(request.getOnlyHidden())) {
            boolQuery.filter(QueryBuilders.termQuery("status", 7));
        } else {
            boolQuery.mustNot(QueryBuilders.termQuery("status", 7));
        }

        if (courseIds != null && !courseIds.isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("id", courseIds));
        }

        if (request != null && StringUtils.isNotEmpty(request.getKeyword())) {
            boolQuery.must(buildKeywordQuery(request.getKeyword()));
            sourceBuilder.sort(SortBuilders.scoreSort().order(SortOrder.DESC));
        }

        if (request != null && StringUtils.isNotEmpty(request.getTags())) {
            boolQuery.filter(QueryBuilders.termsQuery("tagNames", request.getTags()));
        }

        if (request != null && StringUtils.isNotEmpty(request.getResourceType())) {
            boolQuery.filter(QueryBuilders.termQuery("resourceType", request.getResourceType()));
        }
        if (request != null && request.getDifficulty() != null) {
            boolQuery.filter(QueryBuilders.termQuery("difficulty", request.getDifficulty()));
        }
        if (request != null && request.getCourseIdFilter() != null) {
            boolQuery.filter(QueryBuilders.termQuery("id", request.getCourseIdFilter()));
        }

        sourceBuilder.query(boolQuery);
        sourceBuilder.sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC));
        return sourceBuilder;
    }

    private QueryBuilder buildKeywordQuery(String keyword) {
        return QueryBuilders.multiMatchQuery(keyword)
                .field("title", 8.0f)
                .field("tagNamesText", 4.0f)
                .field("intro", 3.0f)
                .field("serviceContent", 2.0f)
                .field("searchText", 1.0f)
                .type(MultiMatchQueryBuilder.Type.BEST_FIELDS);
    }

    private CourseSearchLoginVo toVo(OshCourseEsDocument document) {
        CourseSearchLoginVo vo = new CourseSearchLoginVo();
        vo.setId(document.getId());
        vo.setTitle(document.getTitle());
        vo.setCover(document.getCover());
        vo.setIntro(document.getIntro());
        vo.setServiceContent(document.getServiceContent());
        vo.setPrice(document.getPrice());
        vo.setTPrice(document.getTPrice());
        vo.setType(document.getType());
        vo.setSubCount(document.getSubCount());
        vo.setRemark(document.getRemark());
        vo.setCreateBy(document.getCreateBy());
        vo.setCreateTime(document.getCreateTime());
        vo.setUpdateBy(document.getUpdateBy());
        vo.setUpdateTime(document.getUpdateTime());
        vo.setTotalDuration(document.getTotalDuration());
        vo.setFreeLessonCount(document.getFreeLessonCount());
        vo.setVideoCount(document.getVideoCount());
        vo.setSalesCount(document.getSalesCount());
        vo.setViewCount(document.getViewCount());
        vo.setLikeCount(document.getLikeCount());
        vo.setCommentCount(document.getCommentCount());
        vo.setQuestionCount(document.getQuestionCount());
        vo.setCollectionCount(document.getCollectionCount());
        vo.setRatingScore(document.getRatingScore());
        vo.setFreeType(document.getFreeType());
        vo.setAfterServiceDays(document.getAfterServiceDays());
        vo.setResourceType(document.getResourceType());
        vo.setLevel(document.getLevel());
        vo.setDifficulty(document.getDifficulty());
        vo.setStatus(document.getStatus());
        vo.setExamId(document.getExamId());
        vo.setTagNamesText(document.getTagNamesText());
        vo.setCollectionFlag(0);
        vo.setBuyFlag(0);
        return vo;
    }
}

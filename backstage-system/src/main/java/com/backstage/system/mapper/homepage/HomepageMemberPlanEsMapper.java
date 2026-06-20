package com.backstage.system.mapper.homepage;

import com.backstage.system.config.properties.SearchEsProperties;
import com.backstage.system.domain.homepage.es.HomepageMemberPlanEsDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
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
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class HomepageMemberPlanEsMapper {

    private static final String INDEX_NAME = "homepage_member_plan";
    private static final String MAPPING_RESOURCE = "es/homepage_member_plan_index.json";
    private static final int QUERY_SIZE = 100;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SearchEsProperties searchEsProperties;

    public List<HomepageMemberPlanEsDocument> searchHomepagePlans() throws IOException {
        if (!indexExists()) {
            return new ArrayList<>();
        }

        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        searchRequest.source(buildSearchSource());
        SearchResponse searchResponse;
        try {
            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException exception) {
            if (exception.status() != null && exception.status().getStatus() == 404) {
                return new ArrayList<>();
            }
            throw exception;
        }

        if (searchResponse.isTimedOut()) {
            throw new IllegalStateException("search homepage member plans from es timed out");
        }

        List<HomepageMemberPlanEsDocument> documents = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            documents.add(objectMapper.convertValue(hit.getSourceAsMap(), HomepageMemberPlanEsDocument.class));
        }
        return documents;
    }

    public int bulkUpsert(List<HomepageMemberPlanEsDocument> documents) throws IOException {
        if (documents == null || documents.isEmpty()) {
            return 0;
        }

        BulkRequest bulkRequest = new BulkRequest();
        for (HomepageMemberPlanEsDocument document : documents) {
            bulkRequest.add(new IndexRequest(INDEX_NAME)
                    .id(String.valueOf(document.getId()))
                    .source(objectMapper.writeValueAsString(document), XContentType.JSON));
        }

        BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (bulkResponse.hasFailures()) {
            throw new IllegalStateException("bulk upsert homepage member plans to es failed: "
                    + bulkResponse.buildFailureMessage());
        }
        return documents.size();
    }

    public void upsert(HomepageMemberPlanEsDocument document) throws IOException {
        if (document == null || document.getId() == null) {
            return;
        }

        IndexRequest indexRequest = new IndexRequest(INDEX_NAME)
                .id(String.valueOf(document.getId()))
                .source(objectMapper.writeValueAsString(document), XContentType.JSON);
        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    }

    public void deleteById(Long planId) throws IOException {
        if (planId == null || !indexExists()) {
            return;
        }

        DeleteRequest deleteRequest = new DeleteRequest(INDEX_NAME, String.valueOf(planId));
        restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
    }

    public int deleteAll() throws IOException {
        if (!indexExists()) {
            return 0;
        }

        Request request = new Request("POST", "/" + INDEX_NAME + "/_delete_by_query");
        request.addParameter("refresh", "true");
        request.addParameter("conflicts", "proceed");
        request.setJsonEntity("{\"query\":{\"match_all\":{}}}");

        Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
        return objectMapper.readTree(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8))
                .path("deleted")
                .asInt(0);
    }

    public boolean indexExists() throws IOException {
        return restHighLevelClient.indices().exists(new GetIndexRequest(INDEX_NAME), RequestOptions.DEFAULT);
    }

    public void recreateIndex(String indexDefinitionJson) throws IOException {
        boolean exists = indexExists();
        if (exists) {
            restHighLevelClient.indices().delete(new DeleteIndexRequest(INDEX_NAME), RequestOptions.DEFAULT);
        }
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(INDEX_NAME);
        createIndexRequest.source(indexDefinitionJson, XContentType.JSON);
        restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
    }

    public static String loadMappingJson() throws IOException {
        ClassPathResource resource = new ClassPathResource(MAPPING_RESOURCE);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    private SearchSourceBuilder buildSearchSource() {
        return new SearchSourceBuilder()
                .from(0)
                .size(QUERY_SIZE)
                .timeout(TimeValue.timeValueMillis(searchEsProperties.getFallbackTimeoutMillis()))
                .query(QueryBuilders.boolQuery()
                        .filter(QueryBuilders.termQuery("status", 1))
                        .filter(QueryBuilders.termQuery("deleteFlag", 0)))
                .sort(SortBuilders.fieldSort("sort").order(SortOrder.ASC).missing("_last").unmappedType("integer"))
                .sort(SortBuilders.fieldSort("id").order(SortOrder.ASC).unmappedType("long"));
    }
}

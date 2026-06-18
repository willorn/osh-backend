package com.backstage.system.service.impl.course;

import com.backstage.common.response.PageResponse;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.course.es.OshCourseEsDocument;
import com.backstage.system.domain.course.vo.CourseSearchLoginVo;
import com.backstage.system.domain.user.CurrentUser;
import com.backstage.system.enums.CourseResourceEnum;
import com.backstage.system.mapper.course.OshCourseEsMapper;
import com.backstage.system.mapper.course.OshCourseCollectionMapper;
import com.backstage.system.mapper.course.OshCourseMapper;
import com.backstage.system.mapper.course.OshCourseTagMapper;
import com.backstage.system.request.CourseSearchRequest;
import com.backstage.system.service.course.ICourseManageService;
import com.backstage.system.service.course.IOshCourseEsService;
import com.github.pagehelper.PageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OshCourseEsServiceImpl implements IOshCourseEsService {

    private static final Logger log = LoggerFactory.getLogger(OshCourseEsServiceImpl.class);

    @Autowired
    private OshCourseEsMapper oshCourseEsMapper;

    @Autowired
    private OshCourseMapper oshCourseMapper;

    @Autowired
    private OshCourseCollectionMapper oshCourseCollectionMapper;

    @Autowired
    private OshCourseTagMapper oshCourseTagMapper;

    @Autowired
    private ICourseManageService courseManageService;

    @Override
    public PageResponse<CourseSearchLoginVo> searchCourses(CourseSearchRequest request, Long userId) {
        PageResponse<CourseSearchLoginVo> pageResponse;
        try {
            if (Integer.valueOf(1).equals(request.getCollectionFlag())) {
                pageResponse = searchCollectedCourses(request, userId);
            } else {
                pageResponse = oshCourseEsMapper.searchCourses(request);
            }
        } catch (Exception ex) {
            log.error("search courses from es failed, request={}, userId={}", request, userId, ex);
            throw new IllegalStateException("search courses from es failed", ex);
        }

        List<CourseSearchLoginVo> rows = pageResponse.getRows();
        if (StringUtils.isEmpty(rows)) {
            return pageResponse;
        }

        fillCollectionFlag(rows, userId);
        fillBuyFlag(rows, userId);
        fillResourceTypeDesc(rows, userId);
        convertToExpiryUrls(rows);
        return pageResponse;
    }

    private PageResponse<CourseSearchLoginVo> searchCollectedCourses(CourseSearchRequest request, Long userId) throws Exception {
        if (userId == null) {
            return PageResponse.of(Collections.emptyList(), 0L, request.getPageNum(), request.getPageSize());
        }

        List<Long> collectedCourseIds = oshCourseCollectionMapper.selectActiveCourseIdsByUserId(userId);
        if (StringUtils.isEmpty(collectedCourseIds)) {
            return PageResponse.of(Collections.emptyList(), 0L, request.getPageNum(), request.getPageSize());
        }

        CourseSearchRequest collectionSearchRequest = buildCollectionSearchRequest(request, collectedCourseIds.size());
        PageResponse<CourseSearchLoginVo> searchResponse = oshCourseEsMapper.searchCourses(collectionSearchRequest, collectedCourseIds);
        List<CourseSearchLoginVo> orderedRows = sortRowsByCollectedOrder(searchResponse.getRows(), collectedCourseIds);
        return buildPagedResponse(orderedRows, request.getPageNum(), request.getPageSize());
    }

    @Override
    public int syncAllCoursesToEs() {
        return syncCoursesToEs(false);
    }

    @Override
    public int syncAllCoursesToEsWithoutStatusFilter() {
        return syncCoursesToEs(true);
    }

    private int syncCoursesToEs(boolean includeAllStatuses) {
        int pageNum = 1;
        int pageSize = 200;
        int total = 0;

        // 增量 upsert，避免先 deleteAll 后写回失败导致 ES 索引被清空
        while (true) {
            CourseSearchRequest request = new CourseSearchRequest();
            request.setPageNum(pageNum);
            request.setPageSize(pageSize);

            PageHelper.startPage(pageNum, pageSize);
            List<CourseSearchLoginVo> rows = includeAllStatuses
                    ? oshCourseMapper.pageQueryAllCoursesForEsSync(request)
                    : oshCourseMapper.pageQuerySearchCourse(request, new CurrentUser().getId());
            if (StringUtils.isEmpty(rows)) {
                break;
            }

            List<OshCourseEsDocument> documents = new ArrayList<>(rows.size());
            for (CourseSearchLoginVo row : rows) {
                documents.add(buildEsDocument(row));
            }

            try {
                total += oshCourseEsMapper.bulkUpsertCourses(documents);
            } catch (Exception ex) {
                throw new IllegalStateException("sync courses to es failed", ex);
            }

            if (rows.size() < pageSize) {
                break;
            }
            pageNum++;
        }

        return total;
    }

    @Override
    public void upsertCourseById(Long courseId) {
        if (courseId == null) {
            return;
        }
        try {
            CourseSearchRequest request = new CourseSearchRequest();
            request.setIncludeUnpublished(true);
            request.setCourseIdFilter(courseId);
            request.setPageNum(1);
            request.setPageSize(1);
            PageHelper.startPage(1, 1);
            List<CourseSearchLoginVo> rows = oshCourseMapper.pageQuerySearchCourse(request, null);
            if (StringUtils.isEmpty(rows)) {
                log.warn("upsert course to es skipped, course not found, courseId={}", courseId);
                return;
            }
            oshCourseEsMapper.bulkUpsertCourses(Collections.singletonList(buildEsDocument(rows.get(0))));
            log.info("course upserted to es, courseId={}, status={}", courseId, rows.get(0).getStatus());
        } catch (Exception ex) {
            log.warn("upsert course to es failed, courseId={}", courseId, ex);
        }
    }

    private void fillBuyFlag(List<CourseSearchLoginVo> rows, Long userId) {
        if (userId == null || StringUtils.isEmpty(rows)) {
            return;
        }
        List<Long> courseIds = rows.stream()
                .map(CourseSearchLoginVo::getId)
                .collect(Collectors.toList());
        if (StringUtils.isEmpty(courseIds)) {
            return;
        }

        List<Long> boughtCourseIds = oshCourseMapper.selectUserBoughtCourseIds(userId, courseIds);
        if (StringUtils.isEmpty(boughtCourseIds)) {
            return;
        }
        Set<Long> boughtCourseIdSet = new HashSet<>(boughtCourseIds);

        for (CourseSearchLoginVo row : rows) {
            if (boughtCourseIdSet.contains(row.getId())) {
                row.setBuyFlag(1);
            }
        }
    }

    private void fillCollectionFlag(List<CourseSearchLoginVo> rows, Long userId) {
        if (userId == null || StringUtils.isEmpty(rows)) {
            return;
        }
        List<Long> courseIds = rows.stream()
                .map(CourseSearchLoginVo::getId)
                .collect(Collectors.toList());
        if (StringUtils.isEmpty(courseIds)) {
            return;
        }

        List<Long> collectedCourseIds = oshCourseCollectionMapper.selectActiveCourseIdsByUserIdAndCourseIds(userId, courseIds);
        if (StringUtils.isEmpty(collectedCourseIds)) {
            return;
        }
        Set<Long> collectedCourseIdSet = new HashSet<>(collectedCourseIds);

        for (CourseSearchLoginVo row : rows) {
            if (collectedCourseIdSet.contains(row.getId())) {
                row.setCollectionFlag(1);
            }
        }
    }

    private List<CourseSearchLoginVo> convertToExpiryUrls(List<CourseSearchLoginVo> rows) {
        if (StringUtils.isEmpty(rows)) {
            return rows;
        }
        List<Long> courseIds = rows.stream()
                .map(CourseSearchLoginVo::getId)
                .collect(Collectors.toList());
        Map<Long, String> signedUrlMap = courseManageService.batchGetCourseCoverUrls(courseIds, 60);
        if (signedUrlMap == null) {
            signedUrlMap = Collections.emptyMap();
        }
        for (CourseSearchLoginVo row : rows) {
            String signedUrl = signedUrlMap.get(row.getId());
            if (StringUtils.isNotEmpty(signedUrl)) {
                row.setCover(signedUrl);
            }
        }
        return rows;
    }

    private void fillResourceTypeDesc(List<CourseSearchLoginVo> rows, Long userId) {
        for (CourseSearchLoginVo row : rows) {
            boolean needPurchasedDesc = CourseResourceEnum.CASH_ONLY.getCode().equals(row.getResourceType())
                    || CourseResourceEnum.CASH_POINT.getCode().equals(row.getResourceType());
            if (userId != null && needPurchasedDesc && Integer.valueOf(1).equals(row.getBuyFlag())) {
                row.setResourceTypeDesc("已购买");
                continue;
            }
            CourseResourceEnum resourceEnum = CourseResourceEnum.fromCode(row.getResourceType());
            row.setResourceTypeDesc(resourceEnum == null ? row.getResourceType() : resourceEnum.getDesc());
        }
    }

    private CourseSearchRequest buildCollectionSearchRequest(CourseSearchRequest request, int courseCount) {
        CourseSearchRequest collectionSearchRequest = new CourseSearchRequest();
        collectionSearchRequest.setTags(request.getTags());
        collectionSearchRequest.setKeyword(request.getKeyword());
        collectionSearchRequest.setResourceType(request.getResourceType());
        collectionSearchRequest.setDifficulty(request.getDifficulty());
        collectionSearchRequest.setCollectionFlag(request.getCollectionFlag());
        collectionSearchRequest.setCourseNo(request.getCourseNo());
        collectionSearchRequest.setIncludeUnpublished(request.getIncludeUnpublished());
        collectionSearchRequest.setPageNum(1);
        collectionSearchRequest.setPageSize(courseCount);
        return collectionSearchRequest;
    }

    private List<CourseSearchLoginVo> sortRowsByCollectedOrder(List<CourseSearchLoginVo> rows, List<Long> collectedCourseIds) {
        if (StringUtils.isEmpty(rows) || StringUtils.isEmpty(collectedCourseIds)) {
            return rows;
        }

        Map<Long, Integer> orderMap = new HashMap<>(collectedCourseIds.size());
        for (int i = 0; i < collectedCourseIds.size(); i++) {
            orderMap.put(collectedCourseIds.get(i), i);
        }

        rows.sort((left, right) -> {
            Integer leftOrder = orderMap.get(left.getId());
            Integer rightOrder = orderMap.get(right.getId());
            if (leftOrder == null && rightOrder == null) {
                return 0;
            }
            if (leftOrder == null) {
                return 1;
            }
            if (rightOrder == null) {
                return -1;
            }
            return Integer.compare(leftOrder, rightOrder);
        });
        return rows;
    }

    private PageResponse<CourseSearchLoginVo> buildPagedResponse(List<CourseSearchLoginVo> rows, int pageNum, int pageSize) {
        if (StringUtils.isEmpty(rows)) {
            return PageResponse.of(Collections.emptyList(), 0L, pageNum, pageSize);
        }

        int fromIndex = Math.max((pageNum - 1) * pageSize, 0);
        if (fromIndex >= rows.size()) {
            return PageResponse.of(Collections.emptyList(), rows.size(), pageNum, pageSize);
        }

        int toIndex = Math.min(fromIndex + pageSize, rows.size());
        List<CourseSearchLoginVo> pageRows = new ArrayList<>(rows.subList(fromIndex, toIndex));
        return PageResponse.of(pageRows, rows.size(), pageNum, pageSize);
    }

    private OshCourseEsDocument buildEsDocument(CourseSearchLoginVo row) {
        OshCourseEsDocument document = new OshCourseEsDocument();
        document.setId(row.getId());
        document.setTitle(row.getTitle());
        document.setIntro(row.getIntro());
        document.setServiceContent(row.getServiceContent());
        document.setCover(row.getCover());
        document.setPrice(row.getPrice());
        document.setTPrice(row.getTPrice());
        document.setType(row.getType());
        document.setSubCount(row.getSubCount());
        document.setRemark(row.getRemark());
        document.setCreateBy(row.getCreateBy());
        document.setUpdateBy(row.getUpdateBy());
        document.setTotalDuration(row.getTotalDuration());
        document.setFreeLessonCount(row.getFreeLessonCount());
        document.setVideoCount(row.getVideoCount());
        document.setSalesCount(row.getSalesCount());
        document.setViewCount(row.getViewCount());
        document.setLikeCount(row.getLikeCount());
        document.setCommentCount(row.getCommentCount());
        document.setQuestionCount(row.getQuestionCount());
        document.setCollectionCount(row.getCollectionCount());
        document.setRatingScore(row.getRatingScore());
        document.setFreeType(row.getFreeType());
        document.setAfterServiceDays(row.getAfterServiceDays());
        document.setResourceType(row.getResourceType());
        document.setLevel(row.getLevel());
        document.setDifficulty(row.getDifficulty());
        document.setStatus(row.getStatus());
        document.setExamId(row.getExamId());
        document.setDeleteFlag(row.getDeleteFlag() == null ? 0 : row.getDeleteFlag());
        document.setCreateTime(row.getCreateTime());
        document.setUpdateTime(row.getUpdateTime());

        List<String> tagNames = extractTagNames(row.getId());
        document.setTagNames(tagNames);
        String tagText = String.join(" ", tagNames);
        document.setTagNamesText(tagText);
        document.setSearchText(buildSearchText(row, tagText));
        return document;
    }

    private List<String> extractTagNames(Long courseId) {
        List<Map<String, Object>> tags = oshCourseTagMapper.selectTagsByCourseId(courseId);
        if (StringUtils.isEmpty(tags)) {
            return Collections.emptyList();
        }

        List<String> tagNames = new ArrayList<>(tags.size());
        for (Map<String, Object> tag : tags) {
            Object name = tag.get("name");
            if (name != null && StringUtils.isNotEmpty(String.valueOf(name))) {
                tagNames.add(String.valueOf(name));
            }
        }
        return tagNames;
    }

    private String buildSearchText(CourseSearchLoginVo row, String tagText) {
        StringBuilder builder = new StringBuilder();
        appendSearchField(builder, row.getTitle());
        appendSearchField(builder, row.getIntro());
        appendSearchField(builder, row.getServiceContent());
        appendSearchField(builder, tagText);
        return builder.toString().trim();
    }

    private void appendSearchField(StringBuilder builder, String value) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(value.trim());
    }
}

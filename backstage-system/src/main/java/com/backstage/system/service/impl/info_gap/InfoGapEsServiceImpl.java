package com.backstage.system.service.impl.info_gap;

import com.backstage.common.response.PageResponse;
import com.backstage.system.domain.dto.info_gap.InfoGapESSearchReqDTO;
import com.backstage.system.domain.dto.info_gap.InfoGapSearchReqDTO;
import com.backstage.system.domain.info_gap.OshInfoGap;
import com.backstage.system.domain.info_gap.OshInfoGapEsDocument;
import com.backstage.system.domain.vo.info_gap.InfoGapVO;
import com.backstage.system.mapper.info_gap.OshInfoGapEsMapper;
import com.backstage.system.mapper.info_gap.OshInfoGapMapper;
import com.backstage.system.service.info_gap.IInfoGapEsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class InfoGapEsServiceImpl implements IInfoGapEsService {

    private static final int DEFAULT_SYNC_PAGE_SIZE = 200;

    @Autowired
    private OshInfoGapEsMapper oshInfoGapEsMapper;

    @Autowired
    private OshInfoGapMapper oshInfoGapMapper;

    @Override
    public PageResponse<InfoGapVO> searchInfoGaps(InfoGapESSearchReqDTO request, Long currentUserId) {
        OshInfoGapEsMapper.InfoGapEsSearchResult esResult;
        try {
            esResult = oshInfoGapEsMapper.searchInfoGaps(request);
        } catch (Exception ex) {
            throw new IllegalStateException("search info gaps from es failed", ex);
        }

        return buildSearchResponse(esResult, currentUserId, request == null ? null : request.getKeyword());
    }

    @Override
    public PageResponse<InfoGapVO> searchInfoGaps(InfoGapSearchReqDTO request, Long currentUserId) {
        OshInfoGapEsMapper.InfoGapEsSearchResult esResult;
        try {
            esResult = oshInfoGapEsMapper.searchInfoGaps(request);
        } catch (Exception ex) {
            throw new IllegalStateException("search info gaps from es failed", ex);
        }

        return buildSearchResponse(esResult, currentUserId, request == null ? null : request.getKeyword());
    }

    private PageResponse<InfoGapVO> buildSearchResponse(OshInfoGapEsMapper.InfoGapEsSearchResult esResult,
                                                        Long currentUserId,
                                                        String keyword) {
        List<Long> ids = esResult.getIds();
        if (ids == null || ids.isEmpty()) {
            return PageResponse.of(Collections.emptyList(), 0L, esResult.getPageNum(), esResult.getPageSize());
        }

        List<InfoGapVO> rows = oshInfoGapMapper.selectInfoGapListByIds(ids, currentUserId);
        Map<Long, InfoGapVO> voMap = rows.stream()
                .collect(Collectors.toMap(InfoGapVO::getId, item -> item, (left, right) -> left));

        List<InfoGapVO> sortedRows = ids.stream()
                .map(voMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        rerankRows(sortedRows, keyword);

        return PageResponse.of(sortedRows, esResult.getTotal(), esResult.getPageNum(), esResult.getPageSize());
    }

    private void rerankRows(List<InfoGapVO> rows, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        if (rows == null || rows.isEmpty() || normalizedKeyword.isEmpty()) {
            return;
        }
        rows.sort(Comparator
                .comparingLong((InfoGapVO row) -> calculateKeywordFrequencyScore(row, normalizedKeyword)).reversed()
                .thenComparing(InfoGapVO::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(InfoGapVO::getId, Comparator.nullsLast(Comparator.reverseOrder())));
    }

    private long calculateKeywordFrequencyScore(InfoGapVO row, String keyword) {
        if (row == null) {
            return 0L;
        }
        long total = 0L;
        total += countOccurrences(row.getTitle(), keyword) * 100L;
        total += countOccurrences(row.getContent(), keyword) * 80L;
        total += countOccurrences(joinTags(row), keyword) * 20L;
        total += countOccurrences(row.getTag(), keyword) * 10L;
        return total;
    }

    private String joinTags(InfoGapVO row) {
        StringBuilder sb = new StringBuilder();
        appendTag(sb, row.getTag1());
        appendTag(sb, row.getTag2());
        appendTag(sb, row.getTag3());
        return sb.toString();
    }

    private void appendTag(StringBuilder sb, String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(tag.trim());
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase();
    }

    private int countOccurrences(String text, String keyword) {
        if (text == null || text.trim().isEmpty() || keyword.isEmpty()) {
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

    @Override
    public int syncAllInfoGapsToEs() {
        int pageNum = 1;
        int total = 0;

        try {
            oshInfoGapEsMapper.deleteAllInfoGaps();
            while (true) {
                int offset = (pageNum - 1) * DEFAULT_SYNC_PAGE_SIZE;
                List<OshInfoGap> rows = oshInfoGapMapper.selectPublishedInfoGapPage(offset, DEFAULT_SYNC_PAGE_SIZE);
                if (rows == null || rows.isEmpty()) {
                    break;
                }

                List<OshInfoGapEsDocument> documents = new ArrayList<>(rows.size());
                for (OshInfoGap row : rows) {
                    documents.add(buildEsDocument(row));
                }

                total += oshInfoGapEsMapper.bulkUpsertInfoGaps(documents);
                if (rows.size() < DEFAULT_SYNC_PAGE_SIZE) {
                    break;
                }
                pageNum++;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("sync all info gaps to es failed", ex);
        }

        return total;
    }

    @Override
    public int deleteAllInfoGapsFromEs() {
        try {
            return oshInfoGapEsMapper.deleteAllInfoGaps();
        } catch (Exception ex) {
            throw new IllegalStateException("delete all info gaps from es failed", ex);
        }
    }

    @Override
    public int initSearchIndex() {
        try {
            if (!oshInfoGapEsMapper.indexExists()) {
                throw new IllegalStateException("osh_infogap_search_index does not exist, please create it in Kibana first");
            }
            return syncAllInfoGapsToEs();
        } catch (Exception ex) {
            throw new IllegalStateException("init info gap es index failed", ex);
        }
    }

    @Override
    public void syncInfoGapToEs(Long infoGapId) {
        try {
            OshInfoGap infoGap = oshInfoGapMapper.selectById(infoGapId);
            if (infoGap == null || !Integer.valueOf(4).equals(infoGap.getStatus()) || !Integer.valueOf(0).equals(infoGap.getDeleteFlag())) {
                return;
            }
            oshInfoGapEsMapper.upsertInfoGap(buildEsDocument(infoGap));
        } catch (Exception ex) {
            throw new IllegalStateException("sync info gap to es failed", ex);
        }
    }

    @Override
    public void deleteInfoGapFromEs(Long infoGapId) {
        try {
            oshInfoGapEsMapper.deleteInfoGap(infoGapId);
        } catch (Exception ex) {
            throw new IllegalStateException("delete info gap from es failed", ex);
        }
    }

    private OshInfoGapEsDocument buildEsDocument(OshInfoGap infoGap) {
        OshInfoGapEsDocument document = new OshInfoGapEsDocument();
        document.setId(infoGap.getId());
        document.setUserId(infoGap.getUserId());
        document.setUserName(infoGap.getUserName());
        document.setTitle(infoGap.getTitle());
        document.setContent(infoGap.getContent());
        document.setCategory(infoGap.getTag());
        document.setStatus(infoGap.getStatus());
        document.setGoodCount(infoGap.getGoodCount());
        document.setCollectCount(infoGap.getCollectCount());
        document.setViewCount(infoGap.getViewCount());
        document.setDeleteFlag(infoGap.getDeleteFlag());
        document.setCreateTime(infoGap.getCreateTime());
        document.setUpdateTime(infoGap.getUpdateTime());

        String no = oshInfoGapMapper.selectInfoGapNoById(infoGap.getId());
        document.setNo(no);

        List<Long> tagIds = oshInfoGapMapper.selectTagIdsByInfoGapId(infoGap.getId());
        List<String> tagNames = oshInfoGapMapper.selectTagNamesByInfoGapId(infoGap.getId());
        document.setTagIds(tagIds);
        document.setTagNames(tagNames);

        String tagNamesText = tagNames == null || tagNames.isEmpty() ? "" : String.join(" ", tagNames);
        document.setTagNamesText(tagNamesText);
        document.setSearchText(buildSearchText(
                document.getTitle(),
                document.getContent(),
                document.getCategory(),
                document.getTagNamesText(),
                document.getNo(),
                document.getUserName()
        ));
        return document;
    }

    private String buildSearchText(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(part.trim());
        }
        return sb.toString();
    }
}

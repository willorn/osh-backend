package com.backstage.system.service.info_gap;

import com.backstage.common.response.PageResponse;
import com.backstage.system.domain.dto.info_gap.InfoGapESSearchReqDTO;
import com.backstage.system.domain.vo.info_gap.InfoGapVO;

public interface InfoGapEsService {

    PageResponse<InfoGapVO> searchInfoGaps(InfoGapESSearchReqDTO request, Long currentUserId);

    int syncAllInfoGapsToEs();

    int deleteAllInfoGapsFromEs();

    int initSearchIndex();

    void recreateSearchIndex(String indexDefinitionJson);

    void syncInfoGapToEs(Long infoGapId);

    void deleteInfoGapFromEs(Long infoGapId);
}

package com.backstage.system.service.info_gap;

import com.backstage.common.response.PageResponse;
import com.backstage.system.domain.dto.info_gap.InfoGapESSearchReqDTO;
import com.backstage.system.domain.dto.info_gap.InfoGapSearchReqDTO;
import com.backstage.system.domain.vo.info_gap.InfoGapVO;

public interface IInfoGapEsService {

    PageResponse<InfoGapVO> searchInfoGaps(InfoGapESSearchReqDTO request, Long currentUserId);

    PageResponse<InfoGapVO> searchInfoGaps(InfoGapSearchReqDTO request, Long currentUserId);

    int syncAllInfoGapsToEs();

    int deleteAllInfoGapsFromEs();

    int initSearchIndex();

    void syncInfoGapToEs(Long infoGapId);

    void deleteInfoGapFromEs(Long infoGapId);
}

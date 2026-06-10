package com.backstage.system.controller.info_gap;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.domain.R;
import com.backstage.common.exception.ServiceException;
import com.backstage.common.response.PageResponse;
import com.backstage.system.config.properties.SearchEsProperties;
import com.backstage.system.domain.dto.info_gap.InfoGapESSearchReqDTO;
import com.backstage.system.domain.dto.info_gap.InfoGapSearchReqDTO;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.vo.info_gap.InfoGapVO;
import com.backstage.system.service.info_gap.IInfoGapEsService;
import com.backstage.system.service.info_gap.InfoGapService;
import com.backstage.system.utils.UserContextUtil;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pc/info_gap/es")
public class InfoGapESController {

    private static final Logger log = LoggerFactory.getLogger(InfoGapController.class);

    @Autowired
    private InfoGapService infoGapService;
    @Autowired
    private IInfoGapEsService infoGapEsService;
    @Autowired
    private SearchEsProperties searchEsProperties;

    /**
     * 使用 ES 搜索信息差，失败时回退 MySQL
     */
    @PostMapping("/search")
    @Anonymous
    public R<PageResponse<InfoGapVO>> searchByEs(@RequestBody InfoGapESSearchReqDTO request) {
        if (request.getKeyword() != null) {
            request.setKeyword(request.getKeyword().trim());
            if (request.getKeyword().isEmpty()) {
                request.setKeyword(null);
            }
        }

        if (request.getKeyword() == null) {
            throw new ServiceException("关键字不能为空");
        }

        OshUser currentOshUser = UserContextUtil.getCurrentUser();
        Long currentUserId = currentOshUser == null ? null : currentOshUser.getId();

        if (searchEsProperties.isEnabled()) {
            try {
                return R.ok(infoGapEsService.searchInfoGaps(request, currentUserId));
            } catch (Exception ex) {
                log.warn("info gap es search fallback to mysql, request={}", request, ex);
            }
        }

        InfoGapSearchReqDTO mysqlRequest = new InfoGapSearchReqDTO();
        mysqlRequest.setPageNum(request.getPageNum());
        mysqlRequest.setPageSize(request.getPageSize());
        mysqlRequest.setKeyword(request.getKeyword());

        List<InfoGapVO> infoGapSearchList = infoGapService.searchInfoGap(mysqlRequest, currentUserId);
        PageInfo<InfoGapVO> pageInfo = new PageInfo<>(infoGapSearchList);
        return R.ok(PageResponse.of(pageInfo.getList(), pageInfo.getTotal(), pageInfo.getPageNum(), pageInfo.getPageSize()));
    }

    /**
     * 全量同步信息差到 ES
     */
    @PostMapping("/esSync/all")
    public R<Integer> syncAllInfoGapsToEs() {
        return R.ok(infoGapEsService.syncAllInfoGapsToEs(), "ok");
    }

    /**
     * 全量删除信息差 ES 索引中的数据
     */
    @PostMapping("/esIndex/init")
    public R<Integer> initSearchIndex() {
        return R.ok(infoGapEsService.initSearchIndex(), "ok");
    }

    @PostMapping("/esDelete/all")
    public R<Integer> deleteAllInfoGapsFromEs() {
        return R.ok(infoGapEsService.deleteAllInfoGapsFromEs(), "ok");
    }
}

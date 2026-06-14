package com.backstage.system.controller.info_gap;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.domain.R;
import com.backstage.common.exception.ServiceException;
import com.backstage.common.response.PageResponse;
import com.backstage.system.config.properties.SearchEsProperties;
import com.backstage.system.domain.dto.info_gap.InfoGapESSearchReqDTO;
import com.backstage.system.domain.dto.info_gap.InfoGapSearchReqDTO;
import com.backstage.system.domain.vo.info_gap.InfoGapVO;
import com.backstage.system.service.info_gap.InfoGapEsService;
import com.backstage.system.service.info_gap.InfoGapService;
import com.backstage.system.utils.UserContextUtil;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/pc/info_gap/es")
public class InfoGapESController {

    private static final Logger log = LoggerFactory.getLogger(InfoGapController.class);

    @Autowired
    private InfoGapService infoGapService;
    @Autowired
    private InfoGapEsService infoGapEsService;
    @Autowired
    private SearchEsProperties searchEsProperties;

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

        Long currentUserId = UserContextUtil.getCurrentUserIdSafely();

        if (searchEsProperties.isEnabled()) {
            log.info("infogap 页面开启 ES 查询");
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
     * 全量导入 InfoGap ES 索引信息
     * @return
     */
    @PostMapping("/esSync/all")
    @Anonymous
    public R<Integer> syncAllInfoGapsToEs() {
        return R.ok(infoGapEsService.syncAllInfoGapsToEs(), "ok");
    }

    @PostMapping("/esIndex/init")
    @Anonymous
    public R<Integer> initSearchIndex() {
        return R.ok(infoGapEsService.initSearchIndex(), "ok");
    }

    @PostMapping("/esIndex/recreate")
    @Anonymous
    public R<String> recreateSearchIndex() {
        try {
            ClassPathResource resource = new ClassPathResource("es/osh_infogap_search_index.json");
            String indexDefinitionJson = StreamUtils.copyToString(
                    resource.getInputStream(), StandardCharsets.UTF_8);
            infoGapEsService.recreateSearchIndex(indexDefinitionJson);
            return R.ok("info gap es index recreated, please call /esSync/all to resync data");
        } catch (Exception e) {
            return R.fail("info gap es index recreate failed: " + e.getMessage());
        }
    }

    @PostMapping("/esDelete/all")
    public R<Integer> deleteAllInfoGapsFromEs() {
        return R.ok(infoGapEsService.deleteAllInfoGapsFromEs(), "ok");
    }
}

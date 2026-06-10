package com.backstage.system.service.impl.homepage;

import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.homepage.vo.HotExamVO;
import com.backstage.system.mapper.homepage.OshHomePageExamMapper;
import com.backstage.system.service.common.OssService;
import com.backstage.system.service.homepage.IOshHomePageModulePathService;
import com.backstage.system.service.homepage.IOshHomePageExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页热门考试 Service 实现
 *
 * @author jayTatum
 */
@Service
public class OshHomePageExamServiceImpl implements IOshHomePageExamService {

    @Autowired
    private OshHomePageExamMapper homePageExamMapper;

    @Autowired
    private OssService ossService;

    @Autowired
    private IOshHomePageModulePathService modulePathService;

    @Override
    public List<HotExamVO> getHotExams(int limit) {
        List<HotExamVO> exams = homePageExamMapper.selectHotExams(limit);
        if (exams.isEmpty()) {
            return exams;
        }

        List<Long> examIds = exams.stream().map(HotExamVO::getId).collect(Collectors.toList());
        List<HotExamVO> tagRels = homePageExamMapper.selectTagsByExamIds(examIds);

        Map<Long, List<String>> tagMap = tagRels.stream()
                .collect(Collectors.groupingBy(
                        HotExamVO::getExamId,
                        Collectors.mapping(HotExamVO::getTagName, Collectors.toList())
                ));

        for (HotExamVO vo : exams) {
            List<String> tags = tagMap.getOrDefault(vo.getId(), Collections.emptyList());
            vo.setTags(tags.subList(0, Math.min(2, tags.size())));
            vo.setDetailUrl(modulePathService.getListPath("exam"));
            if (StringUtils.isNotEmpty(vo.getCover())) {
                vo.setCover(ossService.getLimitedUrl(vo.getCover(), 1440));
            }
        }

        return exams;
    }
}

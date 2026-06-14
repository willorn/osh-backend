package com.backstage.system.service.openproject;

import com.backstage.system.domain.openproject.OshOpenProjectTag;
import com.backstage.system.domain.openproject.dto.OpenProjectEditDTO;
import com.backstage.system.domain.openproject.dto.OpenProjectQueryDTO;
import com.backstage.system.domain.openproject.vo.OpenProjectVO;

import java.util.List;
import java.util.Map;

public interface IOshOpenProjectService {

    Map<String, Object> listPage(OpenProjectQueryDTO queryDTO);

    void updateProject(OpenProjectEditDTO dto);

    void incrementClickCount(Long id);

    OpenProjectVO getDetail(Long id);

    List<OshOpenProjectTag> listTags();
}

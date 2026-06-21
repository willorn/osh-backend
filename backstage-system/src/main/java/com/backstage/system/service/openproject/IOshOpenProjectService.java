package com.backstage.system.service.openproject;

import com.backstage.system.domain.openproject.OshOpenProjectTag;
import com.backstage.system.domain.openproject.OshOpenProjectTechComponent;
import com.backstage.system.domain.openproject.dto.OpenProjectEditDTO;
import com.backstage.system.domain.openproject.dto.OpenProjectLeaderTransferDTO;
import com.backstage.system.domain.openproject.dto.OpenProjectQueryDTO;
import com.backstage.system.domain.openproject.dto.OpenProjectTechComponentLibraryDTO;
import com.backstage.system.domain.openproject.vo.OpenProjectVO;

import java.util.List;
import java.util.Map;

public interface IOshOpenProjectService {

    Map<String, Object> listPage(OpenProjectQueryDTO queryDTO);

    void updateProject(OpenProjectEditDTO dto);

    void updateProjectCore(OpenProjectEditDTO dto);

    void updateProjectCollaboration(OpenProjectEditDTO dto);

    void transferLeader(OpenProjectLeaderTransferDTO dto);

    void incrementClickCount(Long id);

    OpenProjectVO getDetail(Long id);

    List<OshOpenProjectTag> listTags();

    List<OshOpenProjectTechComponent> listTechComponentLibrary(String keyword);

    OshOpenProjectTechComponent saveTechComponentLibrary(OpenProjectTechComponentLibraryDTO dto);

    void deleteTechComponentLibrary(Long id);
}

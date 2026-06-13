package com.backstage.system.service.openproject;

import com.backstage.system.domain.openproject.OshOpenProjectSource;
import com.backstage.system.domain.openproject.dto.OpenProjectSourceDTO;

import java.util.List;

public interface IOshOpenProjectSourceService {
    List<OshOpenProjectSource> listSources();

    OshOpenProjectSource saveSource(OpenProjectSourceDTO dto);

    void deleteSource(Long id);

    int syncSource(Long sourceId);

    int syncAllEnabledSources();
}

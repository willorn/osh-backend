package com.backstage.system.service.behavior;

import com.backstage.system.domain.behavior.ContributionCoefficientConfig;

public interface ContributionCoefficientService {
    ContributionCoefficientConfig getConfig();

    ContributionCoefficientConfig saveConfig(ContributionCoefficientConfig config);
}

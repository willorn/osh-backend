package com.backstage.system.service.homepage;

import com.backstage.system.domain.member.OshMemberPlan;

import java.util.List;

public interface IOshHomePageMemberPlanService {

    List<OshMemberPlan> getPlans();

    int syncAllToEs();

    void upsertPlanById(Long planId);

    void deletePlanById(Long planId);

    boolean indexExists();

    void recreateIndex(String indexDefinitionJson);
}

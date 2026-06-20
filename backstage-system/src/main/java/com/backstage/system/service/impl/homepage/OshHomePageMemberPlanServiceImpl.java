package com.backstage.system.service.impl.homepage;

import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.homepage.es.HomepageMemberPlanEsDocument;
import com.backstage.system.domain.member.OshMemberBenefit;
import com.backstage.system.domain.member.OshMemberPlan;
import com.backstage.system.mapper.homepage.HomepageMemberPlanEsMapper;
import com.backstage.system.mapper.member.OshMemberBenefitMapper;
import com.backstage.system.mapper.member.OshMemberPlanMapper;
import com.backstage.system.service.homepage.IOshHomePageMemberPlanService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OshHomePageMemberPlanServiceImpl implements IOshHomePageMemberPlanService, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(OshHomePageMemberPlanServiceImpl.class);
    private static final int PLAN_STATUS_ENABLED = 1;
    private static final int MAX_BENEFIT_SIZE = 3;

    @Resource
    private HomepageMemberPlanEsMapper homepageMemberPlanEsMapper;

    @Resource
    private OshMemberPlanMapper memberPlanMapper;

    @Resource
    private OshMemberBenefitMapper memberBenefitMapper;

    @Override
    public List<OshMemberPlan> getPlans() {
        try {
            List<HomepageMemberPlanEsDocument> documents = homepageMemberPlanEsMapper.searchHomepagePlans();
            List<OshMemberPlan> plans = new ArrayList<>(documents.size());
            for (HomepageMemberPlanEsDocument document : documents) {
                plans.add(toPlan(document));
            }
            return plans;
        } catch (IOException | IllegalStateException exception) {
            throw buildServiceException("查询首页会员套餐失败", exception);
        }
    }

    @Override
    public int syncAllToEs() {
        List<OshMemberPlan> plans = queryAllEnabledPlans();
        if (plans.isEmpty()) {
            try {
                return homepageMemberPlanEsMapper.deleteAll();
            } catch (IOException | IllegalStateException exception) {
                throw buildServiceException("清空首页会员套餐ES数据失败", exception);
            }
        }

        attachEnabledBenefits(plans);
        List<HomepageMemberPlanEsDocument> documents = new ArrayList<>(plans.size());
        for (OshMemberPlan plan : plans) {
            documents.add(toDocument(plan));
        }

        try {
            homepageMemberPlanEsMapper.deleteAll();
            return homepageMemberPlanEsMapper.bulkUpsert(documents);
        } catch (IOException | IllegalStateException exception) {
            throw buildServiceException("全量同步首页会员套餐到ES失败", exception);
        }
    }

    @Override
    public void upsertPlanById(Long planId) {
        if (planId == null) {
            return;
        }

        OshMemberPlan plan = memberPlanMapper.selectById(planId);
        if (plan == null || shouldDeleteFromEs(plan)) {
            deletePlanQuietly(planId);
            return;
        }

        attachEnabledBenefits(Collections.singletonList(plan));
        try {
            homepageMemberPlanEsMapper.upsert(toDocument(plan));
        } catch (IOException | IllegalStateException exception) {
            throw buildServiceException("同步首页会员套餐到ES失败，planId=" + planId, exception);
        }
    }

    @Override
    public void deletePlanById(Long planId) {
        if (planId == null) {
            return;
        }
        try {
            homepageMemberPlanEsMapper.deleteById(planId);
        } catch (IOException exception) {
            throw buildServiceException("删除首页会员套餐ES文档失败，planId=" + planId, exception);
        }
    }

    @Override
    public boolean indexExists() {
        try {
            return homepageMemberPlanEsMapper.indexExists();
        } catch (IOException exception) {
            throw buildServiceException("检查首页会员套餐ES索引失败", exception);
        }
    }

    @Override
    public void recreateIndex(String indexDefinitionJson) {
        try {
            homepageMemberPlanEsMapper.recreateIndex(indexDefinitionJson);
        } catch (IOException exception) {
            throw buildServiceException("重建首页会员套餐ES索引失败", exception);
        }
    }

    @Override
    public void afterPropertiesSet() {
        try {
            if (!homepageMemberPlanEsMapper.indexExists()) {
                log.info("homepage member plan es index not found, recreate and sync automatically");
                homepageMemberPlanEsMapper.recreateIndex(HomepageMemberPlanEsMapper.loadMappingJson());
                syncAllToEs();
            }
        } catch (IOException | ServiceException exception) {
            log.warn("initialize homepage member plan es index failed", exception);
        }
    }

    private List<OshMemberPlan> queryAllEnabledPlans() {
        LambdaQueryWrapper<OshMemberPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshMemberPlan::getDeleteFlag, (byte) 0)
                .eq(OshMemberPlan::getStatus, PLAN_STATUS_ENABLED)
                .orderByAsc(OshMemberPlan::getSort)
                .orderByAsc(OshMemberPlan::getId);
        return memberPlanMapper.selectList(wrapper);
    }

    private void attachEnabledBenefits(List<OshMemberPlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return;
        }

        List<Long> planIds = plans.stream()
                .map(OshMemberPlan::getId)
                .collect(Collectors.toList());
        List<OshMemberBenefit> benefits = memberBenefitMapper.selectEnabledByPlanIds(planIds);
        Map<Long, List<OshMemberBenefit>> benefitMap = benefits == null
                ? Collections.emptyMap()
                : benefits.stream().collect(Collectors.groupingBy(OshMemberBenefit::getPlanId));

        for (OshMemberPlan plan : plans) {
            List<OshMemberBenefit> benefitList =
                    new ArrayList<>(benefitMap.getOrDefault(plan.getId(), Collections.emptyList()));
            if (benefitList.size() > MAX_BENEFIT_SIZE) {
                benefitList = new ArrayList<>(benefitList.subList(0, MAX_BENEFIT_SIZE));
            }
            plan.setBenefits(benefitList);
        }
    }

    private HomepageMemberPlanEsDocument toDocument(OshMemberPlan plan) {
        HomepageMemberPlanEsDocument document = new HomepageMemberPlanEsDocument();
        document.setId(plan.getId());
        document.setPlanCode(plan.getPlanCode());
        document.setPlanName(plan.getPlanName());
        document.setMemberType(plan.getMemberType());
        document.setPeriodType(plan.getPeriodType());
        document.setDurationMonths(plan.getDurationMonths());
        document.setPrice(plan.getPrice());
        document.setOriginalPrice(plan.getOriginalPrice());
        document.setDescription(plan.getDescription());
        document.setMinPurchaseQuantity(plan.getMinPurchaseQuantity());
        document.setMaxPurchaseQuantity(plan.getMaxPurchaseQuantity());
        document.setGrowthCoefficient(plan.getGrowthCoefficient());
        document.setCapPlanCode(plan.getCapPlanCode());
        document.setCapRatio(plan.getCapRatio());
        document.setSort(plan.getSort());
        document.setStatus(plan.getStatus());
        document.setDeleteFlag(plan.getDeleteFlag());
        document.setCreateTime(plan.getCreateTime());
        document.setUpdateTime(plan.getUpdateTime());
        document.setBenefits(toBenefitDocuments(plan.getBenefits()));
        return document;
    }

    private List<HomepageMemberPlanEsDocument.BenefitDocument> toBenefitDocuments(List<OshMemberBenefit> benefits) {
        if (benefits == null || benefits.isEmpty()) {
            return Collections.emptyList();
        }

        List<HomepageMemberPlanEsDocument.BenefitDocument> documents = new ArrayList<>(benefits.size());
        for (OshMemberBenefit benefit : benefits) {
            HomepageMemberPlanEsDocument.BenefitDocument document =
                    new HomepageMemberPlanEsDocument.BenefitDocument();
            document.setId(benefit.getId());
            document.setPlanId(benefit.getPlanId());
            document.setBenefitTitle(benefit.getBenefitTitle());
            document.setBenefitDescription(benefit.getBenefitDescription());
            document.setIcon(benefit.getIcon());
            document.setSort(benefit.getSort());
            document.setStatus(benefit.getStatus());
            document.setDeleteFlag(benefit.getDeleteFlag());
            document.setCreateTime(benefit.getCreateTime());
            document.setUpdateTime(benefit.getUpdateTime());
            documents.add(document);
        }
        return documents;
    }

    private OshMemberPlan toPlan(HomepageMemberPlanEsDocument document) {
        OshMemberPlan plan = new OshMemberPlan();
        plan.setId(document.getId());
        plan.setPlanCode(document.getPlanCode());
        plan.setPlanName(document.getPlanName());
        plan.setMemberType(document.getMemberType());
        plan.setPeriodType(document.getPeriodType());
        plan.setDurationMonths(document.getDurationMonths());
        plan.setPrice(document.getPrice());
        plan.setOriginalPrice(document.getOriginalPrice());
        plan.setDescription(document.getDescription());
        plan.setMinPurchaseQuantity(document.getMinPurchaseQuantity());
        plan.setMaxPurchaseQuantity(document.getMaxPurchaseQuantity());
        plan.setGrowthCoefficient(document.getGrowthCoefficient());
        plan.setCapPlanCode(document.getCapPlanCode());
        plan.setCapRatio(document.getCapRatio());
        plan.setSort(document.getSort());
        plan.setStatus(document.getStatus());
        plan.setDeleteFlag(document.getDeleteFlag());
        plan.setCreateTime(document.getCreateTime());
        plan.setUpdateTime(document.getUpdateTime());
        plan.setBenefits(toBenefits(document.getBenefits()));
        return plan;
    }

    private List<OshMemberBenefit> toBenefits(List<HomepageMemberPlanEsDocument.BenefitDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }

        List<OshMemberBenefit> benefits = new ArrayList<>(documents.size());
        for (HomepageMemberPlanEsDocument.BenefitDocument document : documents) {
            OshMemberBenefit benefit = new OshMemberBenefit();
            benefit.setId(document.getId());
            benefit.setPlanId(document.getPlanId());
            benefit.setBenefitTitle(document.getBenefitTitle());
            benefit.setBenefitDescription(document.getBenefitDescription());
            benefit.setIcon(document.getIcon());
            benefit.setSort(document.getSort());
            benefit.setStatus(document.getStatus());
            benefit.setDeleteFlag(document.getDeleteFlag());
            benefit.setCreateTime(document.getCreateTime());
            benefit.setUpdateTime(document.getUpdateTime());
            benefits.add(benefit);
        }
        return benefits;
    }

    private boolean shouldDeleteFromEs(OshMemberPlan plan) {
        return !Byte.valueOf((byte) 0).equals(plan.getDeleteFlag())
                || !Integer.valueOf(PLAN_STATUS_ENABLED).equals(plan.getStatus());
    }

    private void deletePlanQuietly(Long planId) {
        try {
            homepageMemberPlanEsMapper.deleteById(planId);
        } catch (IOException exception) {
            log.warn("delete homepage member plan from es failed, planId={}", planId, exception);
        }
    }

    private ServiceException buildServiceException(String message, Throwable exception) {
        String detail = exception == null ? "" : exception.getMessage();
        if (detail == null || detail.trim().isEmpty()) {
            return new ServiceException(message);
        }
        return new ServiceException(message + "：" + detail);
    }
}

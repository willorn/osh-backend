package com.backstage.system.service.member.impl;

import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.member.OshMemberOrder;
import com.backstage.system.domain.member.OshMemberPlan;
import com.backstage.system.domain.member.dto.MemberCheckoutDTO;
import com.backstage.system.domain.member.dto.MemberBenefitSaveDTO;
import com.backstage.system.domain.member.dto.MemberPlanConfigDTO;
import com.backstage.system.domain.member.dto.MemberPricingRuleDTO;
import com.backstage.system.domain.member.vo.MemberCenterVO;
import com.backstage.system.domain.member.vo.MemberOrderVO;
import com.backstage.system.domain.member.vo.MemberPayStatusVO;
import com.backstage.system.domain.member.vo.MemberPlanPriceTierVO;
import com.backstage.system.domain.member.vo.MemberStatusVO;
import com.backstage.system.domain.order.OrderStatusResult;
import com.backstage.system.domain.order.enums.OrderStatusEnum;
import com.backstage.system.domain.order.enums.PaymentStatusEnum;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.domain.vo.pay.OrderCheckoutReqVO;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.domain.member.OshMemberBenefit;
import com.backstage.system.mapper.member.OshMemberBenefitMapper;
import com.backstage.system.mapper.member.OshMemberOrderMapper;
import com.backstage.system.mapper.member.OshMemberPlanMapper;
import com.backstage.system.service.member.MemberCenterService;
import com.backstage.system.service.member.MemberEntitlementService;
import com.backstage.system.service.order.OrderCheckoutService;
import com.backstage.system.service.order.OrderService;
import com.backstage.system.utils.UserContextUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MemberCenterServiceImpl implements MemberCenterService {
    private static final String MEMBER_TYPE_VIP = "vip";
    private static final String MEMBER_TYPE_SMALL_CLASS = "small_class";
    private static final int PLAN_STATUS_ENABLED = 1;
    private static final int PAY_STATUS_PENDING = 0;
    private static final int GRANT_STATUS_PENDING = 0;
    private static final int FOUNDER_LEVEL = 6;
    private static final BigDecimal DEFAULT_GROWTH_COEFFICIENT = new BigDecimal("0.90");
    private static final BigDecimal DEFAULT_CAP_RATIO = new BigDecimal("0.95");
    private static final BigDecimal MONTH_GROWTH_COEFFICIENT = new BigDecimal("0.78");
    private static final BigDecimal SMALL_CLASS_GROWTH_COEFFICIENT = new BigDecimal("0.95");
    private static final BigDecimal MAX_GROWTH_COEFFICIENT = new BigDecimal("1.20");
    private static final BigDecimal MAX_CAP_RATIO = new BigDecimal("1.00");

    @Resource
    private OshMemberPlanMapper memberPlanMapper;

    @Resource
    private OshMemberOrderMapper memberOrderMapper;

    @Resource
    private OshMemberBenefitMapper memberBenefitMapper;

    @Resource
    private OrderCheckoutService orderCheckoutService;

    @Resource
    private OrderService orderService;

    @Resource
    private MemberEntitlementService memberEntitlementService;

    @Override
    public MemberCenterVO getCenter(Long userId) {
        MemberStatusVO vip = defaultIfNull(
                memberOrderMapper.selectMemberStatus(userId, MEMBER_TYPE_VIP),
                MEMBER_TYPE_VIP,
                "VIP用户"
        );
        MemberStatusVO smallClass = defaultIfNull(
                memberOrderMapper.selectMemberStatus(userId, MEMBER_TYPE_SMALL_CLASS),
                MEMBER_TYPE_SMALL_CLASS,
                "小班用户"
        );

        MemberCenterVO vo = new MemberCenterVO();
        vo.setVip(vip);
        vo.setSmallClass(smallClass);
        vo.setCurrent(resolveCurrent(vip, smallClass));
        vo.setPlans(listPlans());
        vo.setFounder(UserContextUtil.getCurrentLevelSafely() >= FOUNDER_LEVEL);
        return vo;
    }

    @Override
    public List<OshMemberPlan> listPlans() {
        LambdaQueryWrapper<OshMemberPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshMemberPlan::getDeleteFlag, (byte) 0)
                .eq(OshMemberPlan::getStatus, PLAN_STATUS_ENABLED)
                .orderByAsc(OshMemberPlan::getSort)
                .orderByAsc(OshMemberPlan::getId);
        List<OshMemberPlan> plans = memberPlanMapper.selectList(wrapper);
        attachBenefits(plans, false);
        attachPriceTiers(plans);
        return plans;
    }

    @Override
    public List<OshMemberPlan> listAllPlansForFounder() {
        ensureFounder();
        LambdaQueryWrapper<OshMemberPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshMemberPlan::getDeleteFlag, (byte) 0)
                .orderByAsc(OshMemberPlan::getSort)
                .orderByAsc(OshMemberPlan::getId);
        List<OshMemberPlan> plans = memberPlanMapper.selectList(wrapper);
        attachBenefits(plans, true);
        attachPriceTiers(plans);
        return plans;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlanConfig(Long operatorId, MemberPlanConfigDTO dto) {
        ensureFounder();
        OshMemberPlan plan = memberPlanMapper.selectById(dto.getId());
        if (plan == null || !Byte.valueOf((byte) 0).equals(plan.getDeleteFlag())) {
            throw new ServiceException("会员套餐不存在");
        }
        validateConfig(dto, plan);

        LocalDateTime now = LocalDateTime.now();
        plan.setPlanName(dto.getPlanName());
        plan.setPrice(dto.getPrice());
        plan.setOriginalPrice(dto.getOriginalPrice());
        plan.setDescription(dto.getDescription());
        plan.setMinPurchaseQuantity(defaultQuantity(dto.getMinPurchaseQuantity()));
        plan.setMaxPurchaseQuantity(defaultMaxQuantity(dto.getMaxPurchaseQuantity(), plan.getPeriodType()));
        plan.setSort(dto.getSort() == null ? 0 : dto.getSort());
        plan.setStatus(dto.getStatus() == null ? PLAN_STATUS_ENABLED : dto.getStatus());
        plan.setUpdateBy(operatorId);
        plan.setUpdateTime(now);
        memberPlanMapper.updateById(plan);

        memberBenefitMapper.softDeleteByPlanId(plan.getId(), operatorId);
        List<MemberBenefitSaveDTO> benefits = dto.getBenefits() == null ? Collections.emptyList() : dto.getBenefits();
        int index = 0;
        for (MemberBenefitSaveDTO benefitDto : benefits) {
            OshMemberBenefit benefit = new OshMemberBenefit();
            benefit.setPlanId(plan.getId());
            benefit.setBenefitTitle(benefitDto.getBenefitTitle());
            benefit.setBenefitDescription(benefitDto.getBenefitDescription());
            benefit.setIcon(benefitDto.getIcon());
            benefit.setSort(benefitDto.getSort() == null ? index * 10 : benefitDto.getSort());
            benefit.setStatus(benefitDto.getStatus() == null ? PLAN_STATUS_ENABLED : benefitDto.getStatus());
            benefit.setCreateBy(operatorId);
            benefit.setUpdateBy(operatorId);
            benefit.setCreateTime(now);
            benefit.setUpdateTime(now);
            benefit.setDeleteFlag((byte) 0);
            memberBenefitMapper.insert(benefit);
            index++;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePricingRule(Long operatorId, MemberPricingRuleDTO dto) {
        ensureFounder();
        OshMemberPlan plan = memberPlanMapper.selectById(dto.getId());
        if (plan == null || !Byte.valueOf((byte) 0).equals(plan.getDeleteFlag())) {
            throw new ServiceException("会员套餐不存在");
        }
        validatePricingRule(dto, plan);

        plan.setGrowthCoefficient(defaultGrowthCoefficient(dto.getGrowthCoefficient(), plan));
        plan.setCapPlanCode(normalizeText(dto.getCapPlanCode()));
        plan.setCapRatio(defaultCapRatio(dto.getCapRatio(), plan.getCapPlanCode()));
        plan.setUpdateBy(operatorId);
        plan.setUpdateTime(LocalDateTime.now());
        memberPlanMapper.updateById(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = ServiceException.class)
    public OrderCheckoutRespVO checkout(Long userId, MemberCheckoutDTO dto) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        OshMemberPlan plan = memberPlanMapper.selectById(dto.getPlanId());
        validatePlan(plan);

        int quantity = resolveQuantity(plan, dto.getQuantity());
        int durationMonths = plan.getDurationMonths() * quantity;
        BigDecimal payableAmount = calculatePrice(plan, quantity, loadEnabledCapPlan(plan));
        BigDecimal originalAmount = defaultMoney(plan.getOriginalPrice(), plan.getPrice()).multiply(BigDecimal.valueOf(quantity));
        OrderCheckoutReqVO reqVO = new OrderCheckoutReqVO();
        reqVO.setUserId(userId);
        reqVO.setProductType(ProductTypeEnum.MEMBER.getCode());
        reqVO.setProductId(plan.getId());
        reqVO.setProductName(plan.getPlanName() + " x" + quantity);
        reqVO.setOriginalAmount(originalAmount);
        reqVO.setDiscountAmount(originalAmount.subtract(payableAmount));
        reqVO.setPayableAmount(payableAmount);
        reqVO.setChannel(dto.getChannel());
        reqVO.setUsePoints(false);

        OrderCheckoutRespVO checkout;
        try {
            checkout = orderCheckoutService.checkout(reqVO);
        } catch (ServiceException ex) {
            throw new ServiceException("会员下单失败：" + ex.getMessage());
        }
        memberOrderMapper.insert(buildMemberOrder(userId, plan, checkout.getOrderNo(), durationMonths, originalAmount, payableAmount, quantity));
        return checkout;
    }

    @Override
    public MemberPayStatusVO getPayStatus(Long userId, String orderNo) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        if (StringUtils.isBlank(orderNo)) {
            throw new ServiceException("订单号不能为空");
        }
        OshMemberOrder memberOrder = memberOrderMapper.selectByOrderNoAndUserId(orderNo, userId);
        if (memberOrder == null) {
            throw new ServiceException("会员订单不存在");
        }

        OrderStatusResult orderStatus = orderService.getOrderStatusForUser(orderNo, userId);
        if (isUnifiedPaid(orderStatus) && !isMemberGranted(memberOrder)) {
            memberEntitlementService.handlePaid(orderNo);
            memberOrder = memberOrderMapper.selectByOrderNoAndUserId(orderNo, userId);
        }
        return buildPayStatus(memberOrder, orderStatus);
    }

    @Override
    public List<MemberOrderVO> listOrders(Long userId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        return memberOrderMapper.selectMemberOrders(userId);
    }

    private OshMemberOrder buildMemberOrder(Long userId, OshMemberPlan plan, String orderNo,
                                            Integer durationMonths, BigDecimal originalAmount,
                                            BigDecimal payAmount, Integer quantity) {
        OshMemberOrder order = new OshMemberOrder();
        LocalDateTime now = LocalDateTime.now();
        order.setUserId(userId);
        order.setPlanId(plan.getId());
        order.setOrderNo(orderNo);
        order.setMemberType(plan.getMemberType());
        order.setPlanNameSnapshot(plan.getPlanName() + " x" + quantity);
        order.setPurchaseQuantity(quantity);
        order.setDurationMonths(durationMonths);
        order.setOriginalAmount(originalAmount);
        order.setPayAmount(payAmount);
        order.setPayStatus(PAY_STATUS_PENDING);
        order.setGrantStatus(GRANT_STATUS_PENDING);
        order.setCreateTime(now);
        order.setCreateBy(userId);
        order.setUpdateTime(now);
        order.setUpdateBy(userId);
        order.setDeleteFlag((byte) 0);
        return order;
    }

    private MemberPayStatusVO buildPayStatus(OshMemberOrder memberOrder, OrderStatusResult orderStatus) {
        MemberPayStatusVO status = new MemberPayStatusVO();
        status.setOrderNo(memberOrder.getOrderNo());
        status.setOrderStatus(orderStatus == null ? null : orderStatus.getOrderStatus());
        status.setPaymentStatus(orderStatus == null ? null : orderStatus.getPaymentStatus());
        status.setMemberPayStatus(memberOrder.getPayStatus());
        status.setGrantStatus(memberOrder.getGrantStatus());
        status.setGrantMessage(memberOrder.getGrantMessage());
        status.setPaid(isUnifiedPaid(orderStatus) || Integer.valueOf(1).equals(memberOrder.getPayStatus()));
        status.setGranted(isMemberGranted(memberOrder));
        return status;
    }

    private boolean isUnifiedPaid(OrderStatusResult orderStatus) {
        if (orderStatus == null) {
            return false;
        }
        return orderStatus.isPayStatus()
                || Integer.valueOf(OrderStatusEnum.PAID.getCode()).equals(orderStatus.getOrderStatus())
                || Integer.valueOf(PaymentStatusEnum.SUCCESS.getCode()).equals(orderStatus.getPaymentStatus());
    }

    private boolean isMemberGranted(OshMemberOrder memberOrder) {
        return memberOrder != null
                && Integer.valueOf(1).equals(memberOrder.getPayStatus())
                && Integer.valueOf(1).equals(memberOrder.getGrantStatus());
    }

    private void validatePlan(OshMemberPlan plan) {
        if (plan == null || !Byte.valueOf((byte) 0).equals(plan.getDeleteFlag())) {
            throw new ServiceException("会员套餐不存在");
        }
        if (!Integer.valueOf(PLAN_STATUS_ENABLED).equals(plan.getStatus())) {
            throw new ServiceException("会员套餐已下架");
        }
        if (!MEMBER_TYPE_VIP.equals(plan.getMemberType()) && !MEMBER_TYPE_SMALL_CLASS.equals(plan.getMemberType())) {
            throw new ServiceException("会员套餐类型不正确");
        }
        if (plan.getDurationMonths() == null || plan.getDurationMonths() <= 0) {
            throw new ServiceException("会员套餐时长不正确");
        }
        if (MEMBER_TYPE_SMALL_CLASS.equals(plan.getMemberType()) && plan.getDurationMonths() < 12) {
            throw new ServiceException("小班用户仅支持年付套餐");
        }
        if (plan.getPrice() == null || plan.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("会员套餐价格不正确");
        }
    }

    private int resolveQuantity(OshMemberPlan plan, Integer requestQuantity) {
        int min = defaultQuantity(plan.getMinPurchaseQuantity());
        int max = effectiveMaxPurchaseQuantity(plan, loadEnabledCapPlan(plan));
        int quantity = requestQuantity == null ? min : requestQuantity;
        if (quantity < min) {
            throw new ServiceException("该套餐最少购买 " + min + " 份");
        }
        if (quantity > max) {
            throw new ServiceException("该套餐最多购买 " + max + " 份");
        }
        return quantity;
    }

    private int defaultQuantity(Integer quantity) {
        return quantity == null || quantity < 1 ? 1 : quantity;
    }

    private int defaultMaxQuantity(Integer quantity, String periodType) {
        if (quantity != null && quantity >= 1) {
            return quantity;
        }
        return "year".equals(periodType) ? 5 : 36;
    }

    private void attachBenefits(List<OshMemberPlan> plans, boolean includeDisabled) {
        if (plans == null || plans.isEmpty()) {
            return;
        }
        List<Long> planIds = plans.stream().map(OshMemberPlan::getId).collect(Collectors.toList());
        List<OshMemberBenefit> benefits = includeDisabled
                ? memberBenefitMapper.selectByPlanIds(planIds)
                : memberBenefitMapper.selectEnabledByPlanIds(planIds);
        Map<Long, List<OshMemberBenefit>> benefitMap = benefits == null ? Collections.emptyMap() :
                benefits.stream().collect(Collectors.groupingBy(OshMemberBenefit::getPlanId));
        for (OshMemberPlan plan : plans) {
            plan.setBenefits(benefitMap.getOrDefault(plan.getId(), new ArrayList<>()));
            if (plan.getMinPurchaseQuantity() == null || plan.getMinPurchaseQuantity() < 1) {
                plan.setMinPurchaseQuantity(1);
            }
            if (plan.getMaxPurchaseQuantity() == null || plan.getMaxPurchaseQuantity() < plan.getMinPurchaseQuantity()) {
                plan.setMaxPurchaseQuantity(defaultMaxQuantity(null, plan.getPeriodType()));
            }
        }
    }

    private void attachPriceTiers(List<OshMemberPlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return;
        }
        Map<String, OshMemberPlan> planMap = plans.stream()
                .filter(plan -> plan.getPlanCode() != null)
                .collect(Collectors.toMap(OshMemberPlan::getPlanCode, plan -> plan, (first, second) -> first));
        for (OshMemberPlan plan : plans) {
            OshMemberPlan capPlan = planMap.get(plan.getCapPlanCode());
            int min = defaultQuantity(plan.getMinPurchaseQuantity());
            int max = effectiveMaxPurchaseQuantity(plan, capPlan);
            plan.setEffectiveMaxPurchaseQuantity(max);
            List<MemberPlanPriceTierVO> tiers = new ArrayList<>();
            for (int quantity = min; quantity <= max; quantity++) {
                MemberPlanPriceTierVO tier = new MemberPlanPriceTierVO();
                tier.setQuantity(quantity);
                tier.setDurationMonths(plan.getDurationMonths() * quantity);
                tier.setPrice(calculatePrice(plan, quantity, capPlan));
                tier.setOriginalPrice(defaultMoney(plan.getOriginalPrice(), plan.getPrice()).multiply(BigDecimal.valueOf(quantity)));
                tiers.add(tier);
            }
            plan.setPriceTiers(tiers);
        }
    }

    private BigDecimal calculatePrice(OshMemberPlan plan, int quantity, OshMemberPlan capPlan) {
        BigDecimal price = normalizePrice(geometricPrice(plan.getPrice(), defaultGrowthCoefficient(plan.getGrowthCoefficient(), plan), quantity));
        BigDecimal capAmount = capAmount(plan, capPlan);
        if (capAmount != null && price.compareTo(capAmount) >= 0) {
            price = endingEightBelow(capAmount);
        }
        if (price.compareTo(plan.getPrice()) < 0) {
            price = plan.getPrice();
        }
        return price;
    }

    private BigDecimal geometricPrice(BigDecimal basePrice, BigDecimal coefficient, int quantity) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal factor = BigDecimal.ONE;
        for (int i = 0; i < quantity; i++) {
            total = total.add(basePrice.multiply(factor));
            factor = factor.multiply(coefficient);
        }
        return total;
    }

    private BigDecimal normalizePrice(BigDecimal value) {
        BigDecimal rounded = value.setScale(0, RoundingMode.HALF_UP);
        BigDecimal lower = endingEightFloor(rounded);
        BigDecimal upper = lower.add(BigDecimal.TEN);
        BigDecimal lowerDistance = rounded.subtract(lower).abs();
        BigDecimal upperDistance = upper.subtract(rounded).abs();
        BigDecimal result = lowerDistance.compareTo(upperDistance) <= 0 ? lower : upper;
        return result.setScale(2, RoundingMode.UNNECESSARY);
    }

    private BigDecimal endingEightBelow(BigDecimal capAmount) {
        BigDecimal candidate = normalizePrice(capAmount.subtract(BigDecimal.ONE));
        while (candidate.compareTo(capAmount) >= 0) {
            candidate = candidate.subtract(BigDecimal.TEN);
        }
        return candidate.setScale(2, RoundingMode.UNNECESSARY);
    }

    private BigDecimal endingEightFloor(BigDecimal value) {
        BigDecimal shifted = value.subtract(new BigDecimal("8"));
        BigDecimal tens = shifted.divide(BigDecimal.TEN, 0, RoundingMode.FLOOR);
        return tens.multiply(BigDecimal.TEN).add(new BigDecimal("8"));
    }

    private BigDecimal capAmount(OshMemberPlan plan, OshMemberPlan capPlan) {
        if (capPlan == null || capPlan.getPrice() == null) {
            return null;
        }
        BigDecimal ratio = defaultCapRatio(plan.getCapRatio(), plan.getCapPlanCode());
        return capPlan.getPrice().multiply(ratio);
    }

    private int effectiveMaxPurchaseQuantity(OshMemberPlan plan, OshMemberPlan capPlan) {
        int min = defaultQuantity(plan.getMinPurchaseQuantity());
        int configuredMax = defaultMaxQuantity(plan.getMaxPurchaseQuantity(), plan.getPeriodType());
        BigDecimal capAmount = capAmount(plan, capPlan);
        if (capAmount == null) {
            return configuredMax;
        }
        int max = min;
        for (int quantity = min; quantity <= configuredMax; quantity++) {
            BigDecimal price = geometricPrice(plan.getPrice(), defaultGrowthCoefficient(plan.getGrowthCoefficient(), plan), quantity);
            if (price.compareTo(capAmount) >= 0) {
                break;
            }
            max = quantity;
        }
        return Math.max(min, max);
    }

    private OshMemberPlan loadEnabledCapPlan(OshMemberPlan plan) {
        if (plan == null || normalizeText(plan.getCapPlanCode()) == null) {
            return null;
        }
        LambdaQueryWrapper<OshMemberPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OshMemberPlan::getDeleteFlag, (byte) 0)
                .eq(OshMemberPlan::getStatus, PLAN_STATUS_ENABLED)
                .eq(OshMemberPlan::getPlanCode, plan.getCapPlanCode())
                .last("limit 1");
        return memberPlanMapper.selectOne(wrapper);
    }

    private void validateConfig(MemberPlanConfigDTO dto, OshMemberPlan plan) {
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("套餐价格必须大于0");
        }
        int min = defaultQuantity(dto.getMinPurchaseQuantity());
        int max = defaultMaxQuantity(dto.getMaxPurchaseQuantity(), plan.getPeriodType());
        if (max < min) {
            throw new ServiceException("最大购买数量不能小于最小购买数量");
        }
        if (dto.getPrice() != null) {
            plan.setPrice(dto.getPrice());
            plan.setMinPurchaseQuantity(min);
            plan.setMaxPurchaseQuantity(max);
            OshMemberPlan capPlan = loadEnabledCapPlan(plan);
            if (plan.getCapPlanCode() != null && capPlan == null) {
                throw new ServiceException("封顶参考套餐不存在或未启用");
            }
            int effectiveMax = effectiveMaxPurchaseQuantity(plan, capPlan);
            if (effectiveMax < min) {
                throw new ServiceException("当前封顶套餐价格过低，无法覆盖最小购买数量");
            }
        }
        if (MEMBER_TYPE_SMALL_CLASS.equals(plan.getMemberType()) && plan.getDurationMonths() < 12) {
            throw new ServiceException("小班用户仅支持年付套餐");
        }
    }

    private void validatePricingRule(MemberPricingRuleDTO dto, OshMemberPlan plan) {
        if (plan.getPlanCode() != null && plan.getPlanCode().equals(normalizeText(dto.getCapPlanCode()))) {
            throw new ServiceException("封顶参考套餐不能选择当前套餐");
        }
        BigDecimal growthCoefficient = defaultGrowthCoefficient(dto.getGrowthCoefficient(), plan);
        if (growthCoefficient.compareTo(BigDecimal.ZERO) <= 0 || growthCoefficient.compareTo(MAX_GROWTH_COEFFICIENT) > 0) {
            throw new ServiceException("增长系数必须在 0.01 到 1.20 之间");
        }
        BigDecimal capRatio = defaultCapRatio(dto.getCapRatio(), dto.getCapPlanCode());
        if (capRatio.compareTo(BigDecimal.ZERO) <= 0 || capRatio.compareTo(MAX_CAP_RATIO) > 0) {
            throw new ServiceException("封顶比例必须在 0.01 到 1.00 之间");
        }
        plan.setGrowthCoefficient(growthCoefficient);
        plan.setCapPlanCode(normalizeText(dto.getCapPlanCode()));
        plan.setCapRatio(capRatio);
        OshMemberPlan capPlan = loadEnabledCapPlan(plan);
        if (plan.getCapPlanCode() != null && capPlan == null) {
            throw new ServiceException("封顶参考套餐不存在或未启用");
        }
        int effectiveMax = effectiveMaxPurchaseQuantity(plan, capPlan);
        if (effectiveMax < defaultQuantity(plan.getMinPurchaseQuantity())) {
            throw new ServiceException("当前封顶套餐价格过低，无法覆盖最小购买数量");
        }
    }

    private BigDecimal defaultGrowthCoefficient(BigDecimal value, OshMemberPlan plan) {
        if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
            return value;
        }
        if (plan != null && "month".equals(plan.getPeriodType())) {
            return MONTH_GROWTH_COEFFICIENT;
        }
        if (plan != null && MEMBER_TYPE_SMALL_CLASS.equals(plan.getMemberType())) {
            return SMALL_CLASS_GROWTH_COEFFICIENT;
        }
        return DEFAULT_GROWTH_COEFFICIENT;
    }

    private BigDecimal defaultCapRatio(BigDecimal value, String capPlanCode) {
        if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
            return value;
        }
        return normalizeText(capPlanCode) == null ? BigDecimal.ONE : DEFAULT_CAP_RATIO;
    }

    private String normalizeText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private void ensureFounder() {
        if (UserContextUtil.getCurrentLevelSafely() < FOUNDER_LEVEL) {
            throw new ServiceException("仅创始人可操作会员配置");
        }
    }

    private BigDecimal defaultMoney(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private MemberStatusVO defaultIfNull(MemberStatusVO status, String type, String name) {
        if (status != null) {
            status.setActive(Boolean.TRUE.equals(status.getActive()));
            status.setRemainingDays(status.getRemainingDays() == null ? 0L : status.getRemainingDays());
            return status;
        }
        MemberStatusVO empty = new MemberStatusVO();
        empty.setMemberType(type);
        empty.setMemberName(name);
        empty.setActive(false);
        empty.setRemainingDays(0L);
        return empty;
    }

    private MemberStatusVO resolveCurrent(MemberStatusVO vip, MemberStatusVO smallClass) {
        if (Boolean.TRUE.equals(smallClass.getActive())) {
            return smallClass;
        }
        if (Boolean.TRUE.equals(vip.getActive())) {
            return vip;
        }
        MemberStatusVO normal = new MemberStatusVO();
        normal.setMemberType("user");
        normal.setMemberName("普通用户");
        normal.setActive(true);
        normal.setRemainingDays(0L);
        return normal;
    }
}

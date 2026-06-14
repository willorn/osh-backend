package com.backstage.system.service.impl.tool;

import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.core.redis.RedisCache;
import com.backstage.common.exception.ServiceException;
import com.backstage.common.response.PageResponse;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.domain.order.enums.PurchaseModeEnum;
import com.backstage.system.domain.tool.OshToolPackage;
import com.backstage.system.domain.tool.OshToolPurchaseRecord;
import com.backstage.system.domain.user.OshUserAsset;
import com.backstage.system.domain.user.OshUserAssetRecord;
import com.backstage.system.domain.vo.pay.OrderCheckoutReqVO;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.domain.vo.tool.ToolPurchaseDetailVO;
import com.backstage.system.domain.vo.tool.ToolPurchaseListVO;
import com.backstage.system.domain.vo.tool.ToolQuotaPackageVO;
import com.backstage.system.mapper.tool.OshToolMapper;
import com.backstage.system.mapper.tool.OshToolPackageMapper;
import com.backstage.system.mapper.tool.OshToolPurchaseRecordMapper;
import com.backstage.system.request.tool.ToolQuotaPackageDeleteRequest;
import com.backstage.system.request.tool.ToolQuotaPackageSaveRequest;
import com.backstage.system.mapper.user.OshUserAssetMapper;
import com.backstage.system.mapper.user.OshUserAssetRecordMapper;
import com.backstage.system.request.tool.ToolPurchaseCreateRequest;
import com.backstage.system.request.tool.ToolPurchaseListRequest;
import com.backstage.system.service.order.OrderCheckoutService;
import com.backstage.system.service.tool.ToolPurchaseService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ToolPurchaseServiceImpl implements ToolPurchaseService {

    private static final int PACKAGE_STATUS_ENABLED = 1;
    private static final int PAY_TYPE_CASH = 1;
    private static final int PAY_TYPE_CASH_POINT = 3;
    private static final int CASH_TO_POINT_RATIO = 10;
    private static final int ORDER_STATUS_PENDING = 0;
    private static final int ORDER_STATUS_CANCELLED = 2;
    private static final int GRANT_STATUS_PENDING = 0;
    private static final int GRANT_STATUS_SUCCESS = 1;
    private static final int ASSET_CHANGE_TYPE_INCOME = 0;
    private static final int ASSET_CHANGE_TYPE_EXPENSE = 1;
    private static final int ASSET_CHANGE_SOURCE_BUY_PRODUCT = 3;

    @Autowired
    private OshToolPackageMapper oshToolPackageMapper;

    @Autowired
    private OshToolPurchaseRecordMapper oshToolPurchaseRecordMapper;

    @Autowired
    private OshToolMapper oshToolMapper;

    @Autowired
    private OshUserAssetMapper oshUserAssetMapper;

    @Autowired
    private OshUserAssetRecordMapper oshUserAssetRecordMapper;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private OrderCheckoutService orderCheckoutService;

    @Override
    public ToolPurchaseDetailVO getPurchaseDetail(Long userId) {
        ToolPurchaseDetailVO detailVO = new ToolPurchaseDetailVO();
        Integer remainingCount = userId == null ? 0 : oshToolMapper.selectUserGlobalRemainingCount(userId);
        detailVO.setRemainingCount(remainingCount == null ? 0 : remainingCount);
        return detailVO;
    }

    @Override
    public List<ToolQuotaPackageVO> listQuotaPackages() {
        return oshToolPackageMapper.selectAllPackageVos();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCheckoutRespVO createPurchaseOrder(Long userId, String operator, ToolPurchaseCreateRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("请先登录");
        }
        if (request == null) {
            throw new IllegalArgumentException("下单参数不能为空");
        }
        OshToolPackage quotaPackage = requireValidPackage(request.getPackageId());
        validatePayType(request.getPayType(), quotaPackage);

        if (PAY_TYPE_CASH_POINT == request.getPayType()) {
            deductUserPoints(userId, operator, quotaPackage);
        }

        OrderCheckoutRespVO checkoutResult = orderCheckoutService.checkout(buildCheckoutReqVO(userId, quotaPackage, request.getChannel()));
        OshToolPurchaseRecord record = buildPurchaseRecord(userId, operator, quotaPackage, request, checkoutResult);
        if (oshToolPurchaseRecordMapper.insertToolPurchaseRecord(record) <= 0) {
            throw new ServiceException("新增全局次数购买记录失败");
        }
        return checkoutResult;
    }

    @Override
    public PageResponse<ToolPurchaseListVO> listPurchaseRecords(Long userId, ToolPurchaseListRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("请先登录");
        }
        ToolPurchaseListRequest listRequest = request == null ? new ToolPurchaseListRequest() : request;
        PageHelper.startPage(listRequest.getPageNum(), listRequest.getPageSize());
        List<ToolPurchaseListVO> rows = oshToolPurchaseRecordMapper.selectPurchaseRecordsByUserId(userId);
        PageInfo<ToolPurchaseListVO> pageInfo = new PageInfo<>(rows);
        return PageResponse.of(pageInfo.getList(), pageInfo.getTotal(), pageInfo.getPageNum(), pageInfo.getPageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPendingPurchase(String paymentNo) {
        if (paymentNo == null) {
            return;
        }
        OshToolPurchaseRecord record = oshToolPurchaseRecordMapper.selectByPaymentNo(paymentNo);
        if (record == null) {
            return;
        }
        if (Integer.valueOf(ORDER_STATUS_CANCELLED).equals(record.getOrderStatus())
                || Integer.valueOf(GRANT_STATUS_SUCCESS).equals(record.getGrantStatus())) {
            return;
        }
        if (Integer.valueOf(PAY_TYPE_CASH_POINT).equals(record.getPackagePayTypeSnapshot())
                && record.getPackagePointAmountSnapshot() != null
                && record.getPackagePointAmountSnapshot() > 0) {
            refundUserPoints(record);
        }
        oshToolPurchaseRecordMapper.updateOrderStatusByOrderNo(record.getOrderNo(), ORDER_STATUS_CANCELLED, SYSTEM_CANCEL_OPERATOR);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveQuotaPackage(String operator, ToolQuotaPackageSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("套餐参数不能为空");
        }
        validateQuotaPackageRequest(request);
        OshToolPackage quotaPackage = buildQuotaPackage(operator, request);
        if (request.getId() == null) {
            if (oshToolPackageMapper.insertPackage(quotaPackage) <= 0) {
                throw new ServiceException("新增工具点数套餐失败");
            }
            return quotaPackage.getId();
        }
        if (oshToolPackageMapper.updatePackage(quotaPackage) <= 0) {
            throw new ServiceException("修改工具点数套餐失败");
        }
        return quotaPackage.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuotaPackage(String operator, ToolQuotaPackageDeleteRequest request) {
        if (request == null || request.getPackageId() == null) {
            throw new IllegalArgumentException("套餐ID不能为空");
        }
        if (oshToolPackageMapper.softDeletePackage(request.getPackageId(), operator) <= 0) {
            throw new ServiceException("删除工具点数套餐失败");
        }
    }

    private static final String SYSTEM_CANCEL_OPERATOR = "system_cancel";

    private void validateQuotaPackageRequest(ToolQuotaPackageSaveRequest request) {
        if (request.getPackageName() == null || request.getPackageName().trim().isEmpty()) {
            throw new IllegalArgumentException("套餐名称不能为空");
        }
        if (request.getUseCount() == null || request.getUseCount() <= 0) {
            throw new IllegalArgumentException("工具点数必须大于0");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("现金金额必须大于0");
        }
        if (request.getPayType() == null || (request.getPayType() != PAY_TYPE_CASH && request.getPayType() != PAY_TYPE_CASH_POINT)) {
            throw new IllegalArgumentException("支付类型错误");
        }
    }

    private OshToolPackage buildQuotaPackage(String operator, ToolQuotaPackageSaveRequest request) {
        OshToolPackage quotaPackage = new OshToolPackage();
        quotaPackage.setId(request.getId());
        quotaPackage.setPackageName(request.getPackageName().trim());
        quotaPackage.setUseCount(request.getUseCount());
        quotaPackage.setPrice(request.getPrice());
        quotaPackage.setPointCost(resolveQuotaPackagePointCost(request));
        quotaPackage.setPayType(request.getPayType());
        quotaPackage.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        quotaPackage.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        quotaPackage.setCreateBy(operator);
        quotaPackage.setUpdateBy(operator);
        return quotaPackage;
    }

    private Integer resolveQuotaPackagePointCost(ToolQuotaPackageSaveRequest request) {
        if (request.getPayType() == null || request.getPayType() == PAY_TYPE_CASH) {
            return 0;
        }
        BigDecimal price = request.getPrice() == null ? BigDecimal.ZERO : request.getPrice();
        return price.multiply(BigDecimal.valueOf(CASH_TO_POINT_RATIO))
                .setScale(0, BigDecimal.ROUND_HALF_UP)
                .intValue();
    }

    private OshToolPackage requireValidPackage(Long packageId) {
        if (packageId == null) {
            throw new IllegalArgumentException("套餐ID不能为空");
        }
        OshToolPackage toolPackage = oshToolPackageMapper.selectPackageById(packageId);
        if (toolPackage == null) {
            throw new ServiceException("套餐不存在");
        }
        if (!Integer.valueOf(PACKAGE_STATUS_ENABLED).equals(toolPackage.getStatus())) {
            throw new ServiceException("套餐已停用");
        }
        return toolPackage;
    }

    private void validatePayType(Integer payType, OshToolPackage toolPackage) {
        if (payType == null) {
            throw new IllegalArgumentException("支付方式不能为空");
        }
        if (payType != PAY_TYPE_CASH && payType != PAY_TYPE_CASH_POINT) {
            throw new IllegalArgumentException("支付方式错误");
        }
        if (!payType.equals(toolPackage.getPayType())) {
            throw new ServiceException("支付方式与套餐配置不一致");
        }
    }

    private void deductUserPoints(Long userId, String operator, OshToolPackage toolPackage) {
        int pointCost = toolPackage.getPointCost() == null ? 0 : toolPackage.getPointCost();
        if (pointCost <= 0) {
            throw new ServiceException("套餐积分金额配置错误");
        }
        OshUserAsset userAsset = oshUserAssetMapper.selectById(userId);
        if (userAsset == null || userAsset.getPoints() == null || userAsset.getPoints() < pointCost) {
            throw new ServiceException("积分不足");
        }

        long beforeBalance = userAsset.getPoints();
        userAsset.setPoints(beforeBalance - pointCost);
        oshUserAssetMapper.updateById(userAsset);

        OshUserAssetRecord assetRecord = new OshUserAssetRecord();
        assetRecord.setUserId(userId);
        assetRecord.setChangeType(ASSET_CHANGE_TYPE_EXPENSE);
        assetRecord.setChangeSource(ASSET_CHANGE_SOURCE_BUY_PRODUCT);
        assetRecord.setChangeAmount((long) pointCost);
        assetRecord.setBeforeBalance(beforeBalance);
        assetRecord.setAfterBalance(userAsset.getPoints());
        assetRecord.setRemark("购买全局次数套餐【" + toolPackage.getPackageName() + "】扣减积分");
        oshUserAssetRecordMapper.insert(assetRecord);

        registerAssetCacheRefreshAfterCommit(userId, userAsset.getPoints());
    }

    private void refundUserPoints(OshToolPurchaseRecord record) {
        OshUserAsset userAsset = oshUserAssetMapper.selectById(record.getUserId());
        if (userAsset == null) {
            throw new ServiceException("用户资产不存在，无法退回积分");
        }
        long beforeBalance = userAsset.getPoints() == null ? 0L : userAsset.getPoints();
        long refundPoint = record.getPackagePointAmountSnapshot();
        userAsset.setPoints(beforeBalance + refundPoint);
        oshUserAssetMapper.updateById(userAsset);

        OshUserAssetRecord assetRecord = new OshUserAssetRecord();
        assetRecord.setUserId(record.getUserId());
        assetRecord.setChangeType(ASSET_CHANGE_TYPE_INCOME);
        assetRecord.setChangeSource(ASSET_CHANGE_SOURCE_BUY_PRODUCT);
        assetRecord.setChangeAmount(refundPoint);
        assetRecord.setBeforeBalance(beforeBalance);
        assetRecord.setAfterBalance(userAsset.getPoints());
        assetRecord.setRemark("取消全局次数订单【" + record.getOrderNo() + "】退回积分");
        oshUserAssetRecordMapper.insert(assetRecord);

        registerAssetCacheRefreshAfterCommit(record.getUserId(), userAsset.getPoints());
    }

    @SuppressWarnings("unchecked")
    private void refreshUserAssetCache(Long userId, Long points) {
        Map<String, Object> userMap = redisCache.getCacheObject(OshUserConstants.LOGIN_USER + userId);
        if (userMap == null) {
            return;
        }
        Object assetObject = userMap.get(OshUserConstants.ASSET);
        if (!(assetObject instanceof Map)) {
            return;
        }
        Map<String, String> asset = (Map<String, String>) assetObject;
        asset.put(OshUserConstants.POINTS, String.valueOf(points));
        userMap.put(OshUserConstants.ASSET, asset);
        redisCache.setCacheObject(OshUserConstants.LOGIN_USER + userId, userMap);
    }

    private void registerAssetCacheRefreshAfterCommit(Long userId, Long points) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            refreshUserAssetCache(userId, points);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                refreshUserAssetCache(userId, points);
            }
        });
    }

    private OrderCheckoutReqVO buildCheckoutReqVO(Long userId, OshToolPackage toolPackage, String channel) {
        OrderCheckoutReqVO reqVO = new OrderCheckoutReqVO();
        reqVO.setUserId(userId);
        reqVO.setProductType(ProductTypeEnum.TOOL.getCode());
        reqVO.setProductId(toolPackage.getId());
        reqVO.setProductName("全局工具次数套餐-" + toolPackage.getPackageName());
        reqVO.setPurchaseMode(PurchaseModeEnum.NORMAL.getCode());
        reqVO.setOriginalAmount(defaultAmount(toolPackage.getPrice()));
        reqVO.setPayableAmount(defaultAmount(toolPackage.getPrice()));
        reqVO.setDiscountAmount(BigDecimal.ZERO);
        reqVO.setChannel(channel);
        return reqVO;
    }

    private OshToolPurchaseRecord buildPurchaseRecord(Long userId,
                                                             String operator,
                                                             OshToolPackage toolPackage,
                                                             ToolPurchaseCreateRequest request,
                                                             OrderCheckoutRespVO checkoutResult) {
        OshToolPurchaseRecord record = new OshToolPurchaseRecord();
        record.setOrderNo(checkoutResult.getOrderNo());
        record.setPaymentNo(checkoutResult.getPaymentNo());
        record.setUserId(userId);
        record.setPackageId(toolPackage.getId());
        record.setPackageNameSnapshot(toolPackage.getPackageName());
        record.setPackageUseCountSnapshot(toolPackage.getUseCount());
        record.setPackageCashAmountSnapshot(defaultAmount(toolPackage.getPrice()));
        record.setPackagePointAmountSnapshot(defaultPoint(toolPackage.getPointCost()));
        record.setPackagePayTypeSnapshot(request.getPayType());
        record.setOrderStatus(ORDER_STATUS_PENDING);
        record.setGrantStatus(GRANT_STATUS_PENDING);
        record.setRemark(null);
        record.setCreateBy(operator);
        record.setUpdateBy(operator);
        record.setDeleteFlag(0);
        return record;
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private Integer defaultPoint(Integer pointCost) {
        return pointCost == null ? 0 : pointCost;
    }
}

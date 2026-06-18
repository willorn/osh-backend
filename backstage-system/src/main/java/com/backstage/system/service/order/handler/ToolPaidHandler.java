package com.backstage.system.service.order.handler;

import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.domain.tool.OshToolPurchaseRecord;
import com.backstage.system.mapper.tool.OshToolPurchaseRecordMapper;
import com.backstage.system.mapper.tool.OshToolQuotaMapper;
import com.backstage.system.service.order.OrderPaidHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Component
public class ToolPaidHandler implements OrderPaidHandler {

    private static final Logger log = LoggerFactory.getLogger(ToolPaidHandler.class);
    private static final String SYSTEM_OPERATOR = "system";
    private static final int GRANT_STATUS_SUCCESS = 1;

    @Resource
    private OshToolPurchaseRecordMapper oshToolPurchaseRecordMapper;

    @Resource
    private OshToolQuotaMapper oshToolQuotaMapper;

    @Override
    public String bizType() {
        return ProductTypeEnum.TOOL.getName();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(String orderNo) {
        log.info("工具点数支付成功后置处理开始, orderNo={}", orderNo);
        OshToolPurchaseRecord record = oshToolPurchaseRecordMapper.selectByOrderNo(orderNo);
        if (record == null) {
            log.warn("工具点数支付成功后置处理失败，购买记录不存在, orderNo={}", orderNo);
            throw new ServiceException("工具点数购买记录不存在");
        }
        if (Integer.valueOf(GRANT_STATUS_SUCCESS).equals(record.getGrantStatus())) {
            log.info("工具点数支付成功后置处理跳过，工具点数余额已发放, orderNo={}, recordId={}", orderNo, record.getId());
            return;
        }
        try {
            int updated = oshToolQuotaMapper.increaseUserQuota(
                    record.getUserId(),
                    record.getPackageUseCountSnapshot(),
                    SYSTEM_OPERATOR
            );
            if (updated <= 0) {
                oshToolQuotaMapper.insertUserQuota(
                        record.getUserId(),
                        record.getPackageUseCountSnapshot(),
                        SYSTEM_OPERATOR
                );
            }
            oshToolPurchaseRecordMapper.updateOrderStatusByOrderNo(orderNo, 1, SYSTEM_OPERATOR);
            int success = oshToolPurchaseRecordMapper.updateGrantSuccess(record.getId(), LocalDateTime.now(), SYSTEM_OPERATOR);
            if (success <= 0) {
                throw new ServiceException("更新工具点数购买发放状态失败");
            }
            log.info("工具点数支付成功后置处理完成，工具点数余额发放成功, orderNo={}, userId={}, packageId={}",
                    orderNo, record.getUserId(), record.getPackageId());
        } catch (Exception ex) {
            log.error("工具点数支付成功后置处理异常, orderNo={}, error={}", orderNo, ex.getMessage(), ex);
            oshToolPurchaseRecordMapper.updateGrantFailed(record.getId(), ex.getMessage(), SYSTEM_OPERATOR);
            throw ex;
        }
    }
}

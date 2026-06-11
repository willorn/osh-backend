package com.backstage.system.service.behavior;

import java.math.BigDecimal;

public interface ContributionService {
    void recordContribution(String resourceType, Long resourceId, String resourceName);

    void recordRevenue(String resourceType, Long resourceId, Long orderId, String orderNo,
                       Long buyerUserId, BigDecimal revenueAmount, Long pointAmount, String bizType);
}

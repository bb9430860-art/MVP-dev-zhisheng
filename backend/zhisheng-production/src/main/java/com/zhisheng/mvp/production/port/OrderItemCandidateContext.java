package com.zhisheng.mvp.production.port;

import java.math.BigDecimal;

public record OrderItemCandidateContext(
        Long id,
        Long tenantId,
        Long orderId,
        String orderNo,
        String orderType,
        String customerType,
        Long dealOwnerId,
        String dealOwnerName,
        String itemName,
        String spec,
        String unit,
        BigDecimal quantity,
        String remark,
        String productType,
        String productionStatus,
        BigDecimal productionProgress,
        Long productionRouteInstanceId) {

    public OrderItemProductionContext toProductionContext() {
        return new OrderItemProductionContext(
                id,
                orderId,
                itemName,
                productType,
                quantity,
                productionStatus,
                productionProgress,
                productionRouteInstanceId);
    }
}

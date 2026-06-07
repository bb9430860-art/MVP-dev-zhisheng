package com.zhisheng.mvp.production.dto;

import java.math.BigDecimal;

public record ProductionWorkOrderCandidateResponse(
        Long orderItemId,
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
        Long productionRouteInstanceId,
        boolean hasActiveWorkOrder,
        Long activeWorkOrderId,
        String activeWorkOrderNo) {
}

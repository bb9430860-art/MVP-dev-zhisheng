package com.zhisheng.mvp.production.port;

import java.math.BigDecimal;

public record OrderItemProductionContext(
        Long id,
        Long orderId,
        String itemName,
        String productType,
        BigDecimal quantity,
        String productionStatus,
        BigDecimal productionProgress,
        Long productionRouteInstanceId) {

    public static OrderItemProductionContext notDispatched(
            Long id,
            Long orderId,
            String itemName,
            String productType,
            BigDecimal quantity) {
        return new OrderItemProductionContext(
                id,
                orderId,
                itemName,
                productType,
                quantity,
                "NOT_DISPATCHED",
                BigDecimal.ZERO,
                null);
    }

    public OrderItemProductionContext withProductionFields(
            String productionStatus,
            BigDecimal productionProgress,
            Long productionRouteInstanceId) {
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

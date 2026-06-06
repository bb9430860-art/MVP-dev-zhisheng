package com.zhisheng.mvp.production.dto;

import com.zhisheng.mvp.production.port.OrderItemProductionContext;
import java.math.BigDecimal;

public record OrderItemProductionResponse(
        Long id,
        Long orderId,
        String itemName,
        String productType,
        BigDecimal quantity,
        String productionStatus,
        BigDecimal productionProgress,
        Long productionRouteInstanceId) {

    public static OrderItemProductionResponse from(OrderItemProductionContext orderItem) {
        return new OrderItemProductionResponse(
                orderItem.id(),
                orderItem.orderId(),
                orderItem.itemName(),
                orderItem.productType(),
                orderItem.quantity(),
                orderItem.productionStatus(),
                orderItem.productionProgress(),
                orderItem.productionRouteInstanceId());
    }
}

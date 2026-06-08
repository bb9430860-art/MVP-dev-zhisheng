package com.zhisheng.mvp.production.dto;

public record WorkOrderDispatchContextResponse(
        ProductionWorkOrderResponse workOrder,
        OrderItemProductionResponse orderItem,
        boolean dispatched) {
}

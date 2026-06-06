package com.zhisheng.mvp.production.dto;

public record OrderItemConfigContextResponse(
        OrderItemProductionResponse orderItem,
        Boolean dispatched) {
}

package com.zhisheng.mvp.production.dto;

public record ProductionDispatchResponse(
        Long routeInstanceId,
        Long orderItemId,
        String status,
        Boolean frozen,
        int stepCount) {
}

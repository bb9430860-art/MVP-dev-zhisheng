package com.zhisheng.mvp.production.dto;

public record ProductionStepExecutionResponse(
        Long stepInstanceId,
        Long routeInstanceId,
        String status,
        Integer productionProgress) {
}

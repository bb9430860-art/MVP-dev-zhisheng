package com.zhisheng.mvp.production.dto;

public record ProductionProgressResponse(
        Long routeInstanceId,
        Integer totalSteps,
        Integer completedSteps,
        Integer progress,
        String routeStatus) {
}

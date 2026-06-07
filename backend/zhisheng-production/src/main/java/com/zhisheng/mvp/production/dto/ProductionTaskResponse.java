package com.zhisheng.mvp.production.dto;

public record ProductionTaskResponse(
        Long stepInstanceId,
        Long routeInstanceId,
        Long orderItemId,
        String stepName,
        Integer stepOrder,
        String assignedRole,
        Long assignedUserId,
        String status) {
}

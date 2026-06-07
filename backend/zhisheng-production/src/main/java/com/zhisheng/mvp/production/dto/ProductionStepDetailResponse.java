package com.zhisheng.mvp.production.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductionStepDetailResponse(
        Long stepInstanceId,
        Long routeInstanceId,
        Long orderId,
        Long orderItemId,
        String itemName,
        Long sourceStepTemplateId,
        String stepCodeSnapshot,
        String stepName,
        Integer stepOrder,
        String assignedRole,
        Long assignedUserId,
        Boolean photoRequired,
        Boolean remarkRequired,
        Boolean mobileEnabled,
        BigDecimal estimatedHours,
        String operationInstruction,
        String status,
        Boolean frozen,
        LocalDateTime startedAt,
        Long startedBy,
        LocalDateTime completedAt,
        Long completedBy,
        Boolean canStart,
        Boolean canComplete) {
}

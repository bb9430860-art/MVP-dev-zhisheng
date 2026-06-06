package com.zhisheng.mvp.production.dto;

import java.math.BigDecimal;

public record ProductionSummaryResponse(
        Long orderItemId,
        String productionStatus,
        Long productionRouteInstanceId,
        BigDecimal progress,
        int totalSteps,
        int completedSteps,
        String currentStepName,
        Boolean dispatched,
        Boolean frozen) {
}

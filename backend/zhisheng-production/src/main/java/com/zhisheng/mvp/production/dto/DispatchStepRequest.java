package com.zhisheng.mvp.production.dto;

import java.math.BigDecimal;

public record DispatchStepRequest(
        String clientStepId,
        Long sourceStepTemplateId,
        String stepCode,
        String stepName,
        Integer stepOrder,
        String assignedRole,
        Long assignedUserId,
        Boolean photoRequired,
        Boolean remarkRequired,
        Boolean mobileEnabled,
        BigDecimal estimatedHours,
        String operationInstruction) {
}

package com.zhisheng.mvp.process.dto;

import java.math.BigDecimal;

public record StepTemplateRequest(
        String stepCode,
        String stepName,
        String assignedRole,
        Boolean photoRequired,
        Boolean remarkRequired,
        Boolean mobileEnabled,
        BigDecimal estimatedHours,
        String operationInstruction,
        Boolean enabled) {
}

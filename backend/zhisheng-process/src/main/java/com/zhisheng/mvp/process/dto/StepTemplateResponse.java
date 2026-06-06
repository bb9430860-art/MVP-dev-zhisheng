package com.zhisheng.mvp.process.dto;

import com.zhisheng.mvp.process.entity.ProcessStepTemplate;
import java.math.BigDecimal;

public record StepTemplateResponse(
        Long id,
        Long routeTemplateId,
        String stepCode,
        String stepName,
        Integer stepOrder,
        String assignedRole,
        Boolean photoRequired,
        Boolean remarkRequired,
        Boolean mobileEnabled,
        BigDecimal estimatedHours,
        String operationInstruction,
        Boolean enabled,
        Boolean deleted) {

    public static StepTemplateResponse from(ProcessStepTemplate step) {
        return new StepTemplateResponse(
                step.getId(),
                step.getRouteTemplateId(),
                step.getStepCode(),
                step.getStepName(),
                step.getStepOrder(),
                step.getAssignedRole(),
                step.getPhotoRequired(),
                step.getRemarkRequired(),
                step.getMobileEnabled(),
                step.getEstimatedHours(),
                step.getOperationInstruction(),
                step.getEnabled(),
                step.getDeleted());
    }
}

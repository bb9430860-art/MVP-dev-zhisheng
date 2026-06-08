package com.zhisheng.mvp.production.dto;

import java.util.List;

public record WorkOrderMaterialReadinessStepResponse(
        Long stepTemplateId,
        Integer stepOrder,
        String stepName,
        List<WorkOrderMaterialReadinessItemResponse> materials) {
}

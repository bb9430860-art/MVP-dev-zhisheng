package com.zhisheng.mvp.production.dto;

import java.math.BigDecimal;

public record WorkOrderMaterialReadinessItemResponse(
        Long materialId,
        String materialCode,
        String materialName,
        String spec,
        String unit,
        BigDecimal requiredQty,
        BigDecimal availableQty,
        BigDecimal shortageQty,
        String readinessStatus,
        String readinessMessage,
        String usageStage,
        Long relatedStepTemplateId,
        Long relatedStepInstanceId,
        String quantityRuleSummary,
        String warning,
        String remark) {
}

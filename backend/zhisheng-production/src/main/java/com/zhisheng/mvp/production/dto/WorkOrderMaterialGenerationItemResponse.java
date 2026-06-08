package com.zhisheng.mvp.production.dto;

import java.math.BigDecimal;

public record WorkOrderMaterialGenerationItemResponse(
        Long materialId,
        String materialCode,
        String materialName,
        String spec,
        String unit,
        BigDecimal requiredQty,
        String usageStage,
        Long stepTemplateId,
        String stepName,
        Integer stepOrder,
        Long relatedStepTemplateId,
        Long relatedStepInstanceId,
        String quantityRuleSummary,
        String warning,
        String remark) {
}

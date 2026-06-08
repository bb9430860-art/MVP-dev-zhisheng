package com.zhisheng.mvp.production.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductionWorkOrderMaterialResponse(
        Long id,
        Long materialId,
        String materialCode,
        String materialName,
        String spec,
        String unit,
        BigDecimal requiredQty,
        BigDecimal availableQtySnapshot,
        BigDecimal shortageQty,
        String readinessStatus,
        LocalDateTime readinessCheckedAt,
        String readinessMessage,
        String usageStage,
        Long relatedStepTemplateId,
        Long relatedStepInstanceId,
        String requirementStatus,
        String remark,
        LocalDateTime updatedAt) {
}

package com.zhisheng.mvp.production.dto;

import java.math.BigDecimal;

public record ProductionWorkOrderMaterialRequest(
        Long materialId,
        String materialCode,
        String materialName,
        String spec,
        String unit,
        BigDecimal requiredQty,
        String usageStage,
        Long relatedStepTemplateId,
        Long relatedStepInstanceId,
        String remark) {
}

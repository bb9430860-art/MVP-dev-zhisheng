package com.zhisheng.mvp.process.dto;

import java.math.BigDecimal;

public record StepMaterialRequirementRequest(
        Long materialId,
        String materialCode,
        String materialName,
        String spec,
        String unit,
        BigDecimal baseQtyPerUnit,
        BigDecimal fixedQty,
        BigDecimal lossRate,
        String requiredQtyExpression,
        String usageStage,
        String remark,
        Boolean enabled) {
}

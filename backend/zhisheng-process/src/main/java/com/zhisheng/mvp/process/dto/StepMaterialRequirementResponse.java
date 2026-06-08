package com.zhisheng.mvp.process.dto;

import com.zhisheng.mvp.process.entity.ProcessStepMaterialRequirementTemplate;
import java.math.BigDecimal;

public record StepMaterialRequirementResponse(
        Long id,
        Long routeTemplateId,
        Long stepTemplateId,
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

    public static StepMaterialRequirementResponse from(ProcessStepMaterialRequirementTemplate material) {
        return new StepMaterialRequirementResponse(
                material.getId(),
                material.getRouteTemplateId(),
                material.getStepTemplateId(),
                material.getMaterialId(),
                material.getMaterialCode(),
                material.getMaterialName(),
                material.getSpec(),
                material.getUnit(),
                material.getBaseQtyPerUnit(),
                material.getFixedQty(),
                material.getLossRate(),
                material.getRequiredQtyExpression(),
                material.getUsageStage(),
                material.getRemark(),
                material.getEnabled());
    }
}

package com.zhisheng.mvp.production.dto;

public record WorkOrderMaterialReadinessCreateRequest(
        Long orderItemId,
        Long routeTemplateId,
        ProductionWorkOrderCreateRequest workOrderFields,
        Boolean applyGeneratedMaterials) {
}

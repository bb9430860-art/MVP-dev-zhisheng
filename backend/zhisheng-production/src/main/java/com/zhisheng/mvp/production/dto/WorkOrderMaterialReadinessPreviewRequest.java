package com.zhisheng.mvp.production.dto;

public record WorkOrderMaterialReadinessPreviewRequest(
        Long orderItemId,
        Long routeTemplateId) {
}

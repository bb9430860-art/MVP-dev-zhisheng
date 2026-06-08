package com.zhisheng.mvp.production.dto;

public record WorkOrderMaterialGenerationRequest(
        Long routeTemplateId,
        Boolean replaceExisting) {
}

package com.zhisheng.mvp.production.dto;

import java.util.List;

public record WorkOrderMaterialGenerationResponse(
        List<WorkOrderMaterialGenerationItemResponse> generatedMaterials,
        int generatedCount,
        int replacedCount,
        List<String> warnings) {
}

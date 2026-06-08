package com.zhisheng.mvp.production.dto;

public record WorkOrderMaterialReadinessSummaryResponse(
        int totalLines,
        int readyLines,
        int shortageLines,
        int unlinkedLines,
        int noStockRecordLines) {
}

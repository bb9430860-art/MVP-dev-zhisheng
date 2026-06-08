package com.zhisheng.mvp.production.dto;

import java.math.BigDecimal;
import java.util.List;

public record WorkOrderMaterialReadinessResponse(
        BigDecimal quantitySnapshot,
        List<WorkOrderMaterialReadinessStepResponse> itemsByStep,
        WorkOrderMaterialReadinessSummaryResponse summary) {
}

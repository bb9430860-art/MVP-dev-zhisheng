package com.zhisheng.mvp.inventory.dto;

import java.math.BigDecimal;

public record InventoryAdjustmentRequest(
        Long materialId,
        BigDecimal adjustmentQty,
        String direction,
        String referenceType,
        Long referenceId,
        String reason,
        String remark,
        String idempotencyKey) {
}

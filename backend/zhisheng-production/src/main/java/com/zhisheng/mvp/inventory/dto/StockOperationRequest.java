package com.zhisheng.mvp.inventory.dto;

import java.math.BigDecimal;

public record StockOperationRequest(
        Long materialId,
        BigDecimal qty,
        String referenceType,
        Long referenceId,
        String reason,
        String remark,
        String idempotencyKey) {
}

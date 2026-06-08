package com.zhisheng.mvp.inventory.dto;

import com.zhisheng.mvp.inventory.entity.InventoryTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryTransactionResponse(
        Long id,
        Long materialId,
        String transactionType,
        BigDecimal qty,
        BigDecimal beforeOnHandQty,
        BigDecimal afterOnHandQty,
        BigDecimal beforeReservedQty,
        BigDecimal afterReservedQty,
        String referenceType,
        Long referenceId,
        String reason,
        String remark,
        Long operatorId,
        LocalDateTime occurredAt,
        String idempotencyKey) {

    public static InventoryTransactionResponse from(InventoryTransaction transaction) {
        return new InventoryTransactionResponse(
                transaction.getId(),
                transaction.getMaterialId(),
                transaction.getTransactionType(),
                transaction.getQty(),
                transaction.getBeforeOnHandQty(),
                transaction.getAfterOnHandQty(),
                transaction.getBeforeReservedQty(),
                transaction.getAfterReservedQty(),
                transaction.getReferenceType(),
                transaction.getReferenceId(),
                transaction.getReason(),
                transaction.getRemark(),
                transaction.getOperatorId(),
                transaction.getOccurredAt(),
                transaction.getIdempotencyKey());
    }
}
